package com.portal.controller;


import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.portal.commons.CacheKey;
import com.portal.commons.Constants;
import com.portal.commons.GlobalReturnCode;
import com.portal.commons.GlobalServiceKey;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.listener.CookieListener;
import com.portal.redis.RedisClient;
import com.portal.service.*;
import com.portal.utils.ConstantsUrls;
import com.portal.utils.CookieUtils;
import com.portal.utils.IpUtil;
import com.portal.utils.SessionUtils;
import com.portal.utils.aes.AES;
import com.portal.utils.aes.AesKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import static com.portal.commons.Constants.USERNAME;

/**
 * @Author: zt
 * @Date: 2019/1/2 11:56
 */
@Controller
@RequestMapping(value = "/cipher")
public class SSOController extends BaseController implements CookieListener {

    private static final Logger logger = LoggerFactory.getLogger(SSOController.class);

    @Autowired
    private CheckAccountSafeService checkAccountSafeService;

    @Autowired
    private PlatUserService platUserService;

    @Autowired
    private PlatCipherUserMapService platCipherUserMapService;

    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private RedisClient<String, String> redisClient;

    public static final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApb+GNNCtCS9FXG5RDIYTT1t29w/aukBRmKGst2ZG9N2G7yhC0KbvQGZp5x96Q4zRGSrmXyF5249FWWxqSYNW3kZ8ZUc3HFBmZMJHmKCsLp55Cbi+SVwTPz3a6f7o88qytamweeVtsx1cMogag+FULRY4Vj4DeB81W4NhQntSdX7JHdJWt2ZeCkJs7Dgy3jfo7UqiBwSNQp4qkm1ZTk6g8n+t1mHREAZSnO71/j3IDxEMPE3OQ8sNXsEEonsWRT5HfETXBiyGdLVgzlnzh1i3KiRC93Kr6rf5XngGnmBUV1R6BCkzBMNwKzb9vVSFboVpwFcoTfVQ0aoXB/XjTJZIpQIDAQAB";


    @Autowired
    private CasUserService casUserService;

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private UserInfoService userInfoService;

    @RequestMapping(value = "/login/sso")
    @ResponseBody
    public Map<String, Object> ssoLogin(HttpServletResponse response, HttpServletRequest request, AdminUser adminUser, String rsaSign, String signtime,String companyUuid) {
        //rsaSign= URLDecoder.decode(rsaSign);

        Map<String, String> map = new HashMap<>();
       // rsaSign = rsaSign.replace(" ", "+");//url空格替换
        map.put("bizcontent", adminUser.getUuid() + signtime);
        map.put("sign", rsaSign);
        boolean check = false;
        try {
            check = AlipaySignature.rsaCheckV2(map, PUBLIC_KEY, "utf-8", "RSA2");
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (check == false) {
            return sendBaseErrorMap(ResultCode.TOKEN_CHECKED_FAILD);
        }
        //设置登录状态
        //TODO:增加session,cookie
        UserInfoDomain userInfoDomain=userInfoService.getUserInfo(adminUser.getPhone(),companyUuid);
        if (userInfoDomain == null){
            return sendBaseErrorMap(ResultCode.USER_IS_NOT_EXIST);
        }
        sessionService.updateSessionExpireTime(IpUtil.getIp(), userInfoDomain.getCompanyId(), userInfoDomain.getUuid());
        CookieUtils.writePortalCookie(response, userInfoDomain.getUuid(), this::setCookies);
        //TODO
        redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfoDomain.getUuid()));
        SessionUtils.setSession(request, response, Constants.COMPANY_UUID, userInfoDomain.getCompanyId());
        SessionUtils.setSession(request, response, USERNAME, userInfoDomain.getUuid());
        //response.sendRedirect("/authlogic/login/sso");
        Map<String,Object> map1=new HashMap<>();
        return setMapParam(map1,"userInfo",userInfoDomain).sendBaseNormalMap(map1);
    }

    @RequestMapping(value = "/sso")
    public void sso(HttpServletRequest request,
                    HttpServletResponse response,
                    @RequestParam(value = "platPhoneNumber") String platPhoneNumber,
                    @RequestParam(value = "sign") String sign,
                    @RequestParam(value = "timestamp") long timestamp) {


        try {
            String companyUUid = ConstantsUrls.getConsoleSessionCompanyid(request);
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            //访问间隔太久
            long currentTimestamp = System.currentTimeMillis();
            if (currentTimestamp - timestamp > 30000) {
                writer.write(getHtml("当前接口已过期!"));
                writer.flush();
                return;
            }

            String sign1 = AES.encryptToBase64(platPhoneNumber, AesKey.AES_KEY);
            //签名不正确
            if (!StringUtils.equals(sign1, sign)) {
                writer.write(getHtml("签名不正确!"));
                writer.flush();
                return;
            }

            /**
             * 平台用户的主键id
             */
            Integer platUserId = platUserService.queryPlatUserIdByPhoneNumber(platPhoneNumber);

            //user provision认证
            Map<String, Object> map = checkAccountSafeService.checkAccountLinsence(GlobalServiceKey.user_provision, platUserId);
            int code = (int) map.get(GlobalReturnCode.RETURN_CODE);
            String msg = (String) map.get(GlobalReturnCode.RETURN_MSG);
            //user provision验证失败
            if (code != GlobalReturnCode.MsgCodeEnum.SUCCESS.getCode()) {
                writer.write(getHtml(msg));
                writer.flush();
                return;
            }
            String token = (String) map.get("token");
            Cookie userProvisionCookie = new Cookie(CookieInfo.USERPROVISION_COOKIE_KEY, token);
            userProvisionCookie.setPath("/");
            userProvisionCookie.setMaxAge(CookieInfo.COOKIE_MAX_AGE);
            userProvisionCookie.setHttpOnly(true);
            response.addCookie(userProvisionCookie);
            String accountNumber = platCipherUserMapService.queryCipherUserByPlatUserId(platUserId);


            /*UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(accountNumber, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "用户单点登录", "用户单点登录", new Date(),companyUUid);
            userBehaviorService.insertUserBehaviorInfo(userBehaviorInfo);*/

            //登录
            request.getSession().setAttribute(ConstantsUrls.USER_SESSION_INFO, accountNumber);
            response.sendRedirect("/portal/index");
        } catch (IOException e) {
            logger.error("Enter SSOController.sso() but failed, platPhoneNumber=[{}], sign=[{}], timestamp=[{}]..==", new Object[]{platPhoneNumber, sign, timestamp});
            logger.error(e.getMessage(), e);
        }

    }

    private String getHtml(String content) {

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "\t<title>portal单点登录</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "\t<script type=\"text/javascript\">\n" +
                "\t\talert(\"" + content + "\");\n" +
                "\t\tlocation.href=\"/login\"\n" +
                "\t</script>\n" +
                "\n" +
                "</body>\n" +
                "</html>";
    }


    /**
     * 根据票据获取从账号信息
     *
     * @param ticket
     * @return
     */
    @RequestMapping({"/cas/login/serviceValidate", "/cas/serviceValidate"})
    @ResponseBody
    public void getSubInfo(@RequestParam(value = "ticket") String ticket,
                           HttpServletRequest request,
                           HttpServletResponse response) {

        logger.info("/cas/serviceVaildate========>" + ticket);
        String obj = "";
        if (redisClient.containsKey(ticket)) {
            obj = (String) redisClient.get(ticket);
        }
        if (StringUtils.isEmpty(obj)) {
            String failureResponse = getFailureResponse(ticket);
            wtiteResponse(response, failureResponse);
            return;
        }
        redisClient.remove(ticket);
        CasUserDomain casUserDomain = JSON.parseObject(obj, CasUserDomain.class);

        if (casUserDomain == null || StringUtils.isEmpty(casUserDomain.getSubAccount()) || StringUtils.isEmpty(casUserDomain.getUuid())) {
            String failureResponse = getFailureResponse(ticket);
            wtiteResponse(response, failureResponse);
            return;
        }

        UserInfoDomain userInfoDomain = new UserInfoDomain();
        userInfoDomain.setUserId(casUserDomain.getUuid());
        CasUserInfoDomain casUserInfoDomain = casUserService.getCasUserByUuid(userInfoDomain);

        logger.info("/cas/login/serviceVaildate========>" + "进入子账号");

        response.addHeader("Content-Type", "application/xml;charset=UTF-8");
        response.addHeader("Content-Language", "zh-CN");
        PrintWriter out = null;
        try {
            out = response.getWriter();

            String successResponse = getSuccessResponse(casUserDomain, casUserInfoDomain);

            logger.info(successResponse);

            out.write(successResponse);

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }

    }

    private void wtiteResponse(HttpServletResponse response, String msg) {
        response.addHeader("Content-Type", "application/xml;charset=UTF-8");
        response.addHeader("Content-Language", "zh-CN");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            logger.info(msg);
            out.write(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }


    /**
     * cas ticket 认证成功后返回认证成功的信息
     *
     * @param casUserInfoDomain 子账号
     * @param casUserDomain     cas对象
     * @return
     */
    private String getSuccessResponse(CasUserDomain casUserDomain, CasUserInfoDomain casUserInfoDomain) {
        String s = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>\n" +
                "    <cas:authenticationSuccess>\n" +
                "        <cas:user>" + casUserDomain.getSubAccount() + "</cas:user>\n" +
                "           <cas:attributes>\n" +
                "               <cas:uuid>" + casUserDomain.getUuid() + "</cas:uuid>\n" +
                "               <cas:userName>" + casUserInfoDomain.getUserName() + "</cas:userName>\n" +
                "               <cas:jonNo>" + casUserInfoDomain.getJonNo() + "</cas:jonNo>\n" +
                "               <cas:email>" + casUserInfoDomain.getMail() + "</cas:email>\n" +
                "               <cas:phone>" + casUserInfoDomain.getPhone() + "</cas:phone>\n" +
                "               <cas:job>" + casUserInfoDomain.getJob() + "</cas:job>\n" +
                "               <cas:groups>" + casUserInfoDomain.getGroups() + "</cas:groups>\n" +
                "           </cas:attributes>" +
                "    </cas:authenticationSuccess>\n" +
                "</cas:serviceResponse>";

        return s;
    }


    /**
     * 获取cas ticket 认证失败的信息
     *
     * @param msg
     * @return
     */
    private String getFailureResponse(String msg) {
        String s = "<cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>  \n" +
                "    <cas:authenticationFailure code=\"INVALID_TICKET\">  \n" +
                "ticket " + msg + " is null" +
                "    </cas:authenticationFailure>  \n" +
                "</cas:serviceResponse>";
        return s;
    }

    @Override
    public void setCookies(String key, String value) {
        redisClient.put(key, value);
    }
}
