package com.portal.controller;

import com.alibaba.dubbo.common.URL;
import com.alibaba.fastjson.JSON;
import com.portal.commons.AppType;
import com.portal.commons.Constants;
import com.portal.commons.ConstantsCMP;
import com.portal.commons.ReturnCode;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.saml.OpenSamlImplementation;
import com.portal.saml.entity.SamlEntity;
import com.portal.service.*;
import com.portal.utils.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sun.misc.BASE64Decoder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;

import static com.portal.commons.Constants.SERVICEURL;
import static com.portal.commons.Constants.USERNAME;
import static com.portal.enums.ResultCode.APPLICATION_RULE_NOT_EXIT;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * TODO:
 * create by shizhao at 2019-12-9
 *
 * @author: shizhao
 * @since: 2019-12-9 10:10
 */
@Controller
@RequestMapping(value = "/cipher/saml")
public class SamlController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(SamlController.class);

    @Autowired
    private SamlService samlService;

    @Resource
    private OpenSamlImplementation openSamlImplementation;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JudgeLimit judgeLimit;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private PortalService portalService;

    @Autowired
    private SubAccountRuleService subAccountRuleService;

    @Autowired
    private CasAndRdsgConfigService casAndRdsgConfigService;

    @Autowired
    private SystemConfigInfoService systemConfigInfoService;

    private static Base64 UNCHUNKED_ENCODER = new Base64(0, new byte[]{'\n'});
    @RequestMapping(value = "/login")
    public void login(HttpServletResponse response, HttpServletRequest request,
                      @RequestParam(value = "SAMLRequest") String saml, @RequestParam(value = "RelayState", required = false) String relayState) {

        //入参校验
        if (StringUtils.isEmpty(saml)) {
            logger.info("params is null by saml saml={{}}", saml);
            ResponseUtils.sendResultMap(response, ResultCode.SAMLPARAMS_IS_NULL);
            return;
        }
//        try {
//            saml= URLDecoder.decode(saml,"utf-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        //decode saml的参数
        BASE64Decoder decoder = new BASE64Decoder();
        String samlEncode;
        try {
            samlEncode = new String(decoder.decodeBuffer(saml), "UTF-8");
        } catch (IOException e) {
            logger.info("samlRequst转String 失败 ");
            logger.error(e.getMessage(), e);
            ResponseUtils.sendResultMap(response, ResultCode.SAML_REGIST_IS_ERROR);
            return;
        }
        //判断是否乱码 如果乱码采用另一种方式
        if (isMessyCode(samlEncode)){
            byte[] bytes=UNCHUNKED_ENCODER.decode(saml);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InflaterOutputStream iout = new InflaterOutputStream(out, new Inflater(true));
            try {
                iout.write(bytes);
                iout.finish();
            } catch (IOException e) {
                logger.info("samlRequst转String 失败 ");
                logger.error(e.getMessage(), e);
                ResponseUtils.sendResultMap(response, ResultCode.SAML_REGIST_IS_ERROR);
            }

            samlEncode= new String(out.toByteArray(), UTF_8);
        }
        //生成saml的Authrequest对象
        AuthnRequest authnRequest = (AuthnRequest) openSamlImplementation.transferXML2SAMLObject(samlEncode);

        String acs = authnRequest.getAssertionConsumerServiceURL();

        ApplicationInfoDomain applicationInfoDomain = applicationService.getApplicationInfoByAssertionConsumerServiceURL(acs);
        if (applicationInfoDomain == null) {
            logger.info("未找到添加应用");
            ResponseUtils.sendResultMap(response, ResultCode.APPLICATION_NO_ADD);
            return;
        }

        if (!samlService.isSamlRegistInLocal(request, samlEncode)) {
            logger.info("params is null by saml saml={{}}", samlEncode);
            ResponseUtils.sendResultMap(response, ResultCode.SAML_REGIST_IS_ERROR);
            return;
        }

        //获取该浏览器的session中的username
       //String userName = (String) SessionUtils.getSessionByName(request, USERNAME);

       String userName="00335c0fe66040ceac03b694c7337548";
        //判断用户是否登录
        //如果该值不为空携带票据，發佈事件 重定向到客户端
        if (StringUtils.isNotEmpty(userName)) {
            //判断是否拥有权限
            Boolean limit = judgeLimit.isOwnAppLimit(userName, String.valueOf(applicationInfoDomain.getId()));
            if (!limit) {
                ResponseUtils.sendResultMap(response, ResultCode.USER_NO_LIMIT);
                return;
            }

            //根据uuId获取userDomain
            UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(userName);
            //获取从账号的配置规则

            PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(new PortalApplyInfo(applicationInfoDomain.getApplicationId()));
            //如果参数为空直接返回应用从账号规则未设置
            if (portalApplyInfo == null || org.apache.commons.lang.StringUtils.isEmpty(portalApplyInfo.getAssociatedAccount())) {
                ResponseUtils.sendResultMap(response, APPLICATION_RULE_NOT_EXIT, Constants.TYPE, Constants.CAS_TYPE);
                return;
            }
            //根据规则获取从账号
            String subAccount = subAccountRuleService.getSubAccountRule(userInfoDomain, portalApplyInfo.getAssociatedAccount(), applicationInfoDomain.getApplicationId());

            //sonAccount为空直接返回错误信息
            if (org.apache.commons.lang.StringUtils.isEmpty(subAccount)) {
                ResponseUtils.sendResultMap(response, ResultCode.USER_NO_SUB);
                return;
            }
            if (samlService.sendIdentityInfo(new SamlEntity(samlEncode, request, response, userName, applicationInfoDomain, subAccount,relayState))) {
                return;
            }
            ;
            ResponseUtils.sendResultMap(response, ResultCode.SAML_RETURN_ERROR);
            return;
        }

        //将SAML认证的标识存入session
        request.getSession().setAttribute(SERVICEURL, AppType.SAML_TYPE + "-" + saml);

        String authUrl = casAndRdsgConfigService.getCasServerAuthUrl();
        //定义StringBuffer字符串 重转向到IAM认证首页
        String stringBuffer = null;
        try {
            stringBuffer = authUrl;
            response.sendRedirect(stringBuffer);
        } catch (IOException e) {
            //打印错误
            logger.error("System error by serviceUrl ,this stringBuffer={}", stringBuffer);
            logger.error(e.getMessage(), e);
        }
        return;

    }


    /**
     * saml 单点回调地址
     *
     * @param response
     * @param request
     * @param saml
     * @param relayState
     */
    @RequestMapping(value = "/sso")
    public void sso(HttpServletResponse response, HttpServletRequest request,
                    @RequestParam(value = "SAMLRequest") String saml,
                    @RequestParam(value = "RelayState", required = false) String relayState) {

        SystemConfigInfo systemConfigInfo = systemConfigInfoService.getSystemConfigInfo();

        try {
            saml= URLEncoder.encode(saml,"utf-8");
            response.sendRedirect(systemConfigInfo.getSamlLoginUrl() + "?SAMLRequest=" + saml +"&RelayState="+relayState);
           // response.sendRedirect( "http://127.0.0.1:8443/cipher/saml/login?SAMLRequest=" + saml +"&RelayState="+relayState);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * 判断字符是否是中文
     *
     * @param c 字符
     * @return 是否是中文
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    public static boolean isMessyCode(String strName) {
        Pattern p = Pattern.compile("\\s*|t*|r*|n*");
        Matcher m = p.matcher(strName);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = ch.length;
        float count = 0;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!Character.isLetterOrDigit(c)) {
                if (!isChinese(c)) {
                    count = count + 1;
                }
            }
        }
        float result = count / chLength;
        if (result > 0.4) {
            return true;
        } else {
            return false;
        }

    }
}
