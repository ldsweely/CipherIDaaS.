package com.portal.controller;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.portal.commons.CacheKey;
import com.portal.commons.Constants;
import com.portal.commons.ConstantsCMP;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.publistener.UserBehaviorPublistener;
import com.portal.redis.RedisClient;
import com.portal.service.*;
import com.portal.token.CheckToken;
import com.portal.utils.HttpClientUtils;
import com.portal.utils.IpUtil;
import com.portal.utils.MapUtil;
import com.portal.utils.ResponseStatus;
import com.portal.utils.aes.AES;
import com.portal.utils.aes.AesKey;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.*;

import static com.portal.enums.ResultCode.*;

;


@Controller
@RequestMapping("/cipher/oidc")
public class OidcController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(OidcController.class);

    @Autowired
    private NcResService ncResService;

    @Autowired
    private PortalService portalService;


    @Autowired
    private UserInfoService userInfoService;


    @Autowired
    private UserBehaviorService userBehaviorService;


    @Autowired
    private ApplicationAudithService applicationAudithService;


    @Autowired
    private RedisClient<String, Object> redisClient = new RedisClient<>();

    @Autowired
    private SubAccountRuleService subAccountRuleService;

    @Autowired
    private SystemConfigInfoService systemConfigInfoService;

    @Autowired
    private UserBehaviorPublistener userBehaviorPublistener;

    @Autowired
    private UserPathService userPathService;



    /**
     * 改造DSG应用跳转
     *
     * @param companyUuid
     * @param uuid
     */

    // @CheckToken
    @RequestMapping(value = "doOidcAuthorize", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getOidcToken(@RequestParam(value = "companyUUid") String companyUuid,
                                            @RequestParam(value = "userId") String uuid,
                                            String applyId, HttpServletResponse response) throws UnknownHostException, InterruptedException {
        Map<String, Object> map = new HashMap<String, Object>();
        UserBehaviorInfo record = null;
        AppAuditInfo appAuditInfo = null;
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(uuid);
        try {
            PortalApplyInfo applyInfo = new PortalApplyInfo();
            applyInfo.setClientId(applyId);
            PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(applyInfo);
            portalApplyInfo = portalService.selectPortalInfo(portalApplyInfo);
            SubClientInfoDomain subClientInfoDomain = userInfoService.getSubNameInfo(uuid, applyId);
            if (null == subClientInfoDomain) {
                map.put("return_code", ResponseStatus.FAIL);
                map.put("return_msg", "未配置应用从账号，请联系管理员");
                return map;
            }
            String clientId = portalApplyInfo.getClientId();
            String account = subClientInfoDomain.getSubAccount();
            account = AES.encryptToBase64(account, AesKey.AES_KEY);
            String newApplyUrl = portalApplyInfo.getRedirectUrl() + "?userName=" + account + "&clientId=" + clientId;
            logger.info("dsg管理后台跳转的地址---------------------------------------" + newApplyUrl);
            map.put("applyUrl", newApplyUrl);
            map.put("return_code", ResponseStatus.SUCCESS);
            try {
                /*record = new UserBehaviorInfo(uuid, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), subClientInfoDomain.getSubAccount() + "登录子系统" + portalApplyInfo.getApplyName() + "成功", "登录子系统", new Date(), companyUuid);
                userBehaviorService.insertUserBehaviorInfo(record);*/
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(uuid, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), subClientInfoDomain.getSubAccount() + "登录子系统" + portalApplyInfo.getApplyName() + "成功", companyUuid);
                userBehaviorPublistener.publish(userBehaviorInfo);
                String userInfo = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
                appAuditInfo = new AppAuditInfo(Integer.valueOf(portalApplyInfo.getId()),
                        userInfo, 1, subClientInfoDomain.getSubAccount() + "登录子系统" + portalApplyInfo.getApplyName() + "成功", companyUuid);
                applicationAudithService.insertAppAuditInfo(appAuditInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            map.put("return_code", ResponseStatus.FAIL);
            map.put("return_msg", "应用异常，联系管理员");
        }
        return map;
    }

    /**
     * 点击应用获取应用信息
     *
     * @param applyId
     * @param companyUuid
     * @param uuid
     */
    @RequestMapping(value = "/doAuthOidcAuthorize", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> doAtuhOidcAuthorize(
            @RequestParam(value = "applyId") String applyId,
            @RequestParam(value = "companyUUid") String companyUuid,
            @RequestParam(value = "userId") String uuid,
            HttpServletResponse response) {
        if (applyId.isEmpty() || uuid.isEmpty()) {
          return   sendBaseErrorMap(PARAMETER_FAILURE);
        }
        PortalApplyInfo applyInfo = new PortalApplyInfo();
        applyInfo.setClientId(applyId);
        PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(applyInfo);
        if (null == portalApplyInfo) {
            return sendBaseErrorMap(APPLICATION_NO_EXIT);

        }
        if (null == portalApplyInfo.getAssociatedAccount() || portalApplyInfo.getAssociatedAccount().isEmpty()) {
            return sendBaseErrorMap(APPLICATION_RULE_NOT_EXIT);
        }

        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(uuid);
        if (null == userInfoDomain) {
            return sendBaseErrorMap(USER_IS_NOT_EXIST);
        }
        return subAccountRuleService.getSubAccountInfo(userInfoDomain, portalApplyInfo);
    }

    /**
     * 检验子账号和密码是否正确
     *
     * @param subName
     * @param subPwd
     * @param applyId
     * @param uuid
     * @param companyUuid
     */
    @CheckToken
    @RequestMapping(value = "checkUser", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> checkUser(@RequestParam(value = "subName") String subName,
                                         @RequestParam(value = "subPwd") String subPwd,
                                         @RequestParam(value = "applyId") String applyId,
                                         @RequestParam(value = "companyUUid") String companyUuid,
                                         @RequestParam(value = "userId") String uuid) {
        if (applyId.isEmpty() || uuid.isEmpty() || subName.isEmpty() || companyUuid.isEmpty() || subPwd.isEmpty()) {
           return sendBaseErrorMap(PARAMETER_FAILURE);
        }
        PortalApplyInfo applyInfo = new PortalApplyInfo();
        applyInfo.setClientId(applyId);
        PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(applyInfo);
        if (null == portalApplyInfo) {
            return sendBaseErrorMap(APPLICATION_NO_EXIT);
        }

        if (portalApplyInfo.getAssociatedAccount().isEmpty()) {
            return sendBaseErrorMap(APPLICATION_RULE_NOT_EXIT);
        }

        return subAccountRuleService.chenckSubAccountAndPwd(uuid, portalApplyInfo, subName, subPwd);
    }


    /**
     * 管理后台跳转
     *
     * @param uuid
     * @param companyUUid
     */
    @CheckToken
    @RequestMapping(value = "doOidcConsoleAuthorize", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getOidcConsoleToken(@RequestParam(value = "userId") String uuid,
                                                   @RequestParam(value = "companyUUid") String companyUUid, HttpServletResponse response) {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Long timestamp = System.currentTimeMillis();
            SortedMap<String, Object> parameterMap = new TreeMap<>();
            parameterMap.put("uuid", uuid);
            parameterMap.put("companyUUid", companyUUid);
            parameterMap.put("timestamp", timestamp);
            StringBuffer sb = new StringBuffer();
            Set es = parameterMap.entrySet();  //所有参与传参的参数按照accsii排序（升序）
            Iterator it = es.iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object v = entry.getValue();
                //空值不传递，不参与签名组串
                if (null != v && !"".equals(v)) {
                    sb.append(v);
                }
            }
            String param = MapUtil.getMapToString(parameterMap);
            String cipherParam = AES.encryptToBase64(param, AesKey.AES_KEY);
            redisClient.put(CacheKey.getCacheKeyCipherConsoleUserInfo(uuid), cipherParam);
            SystemConfigInfo systemConfigInfo=systemConfigInfoService.getSystemConfigInfo();
            String newApplyUrl = systemConfigInfo.getConsoleRedirectUrl() + "?ticket=" + URLEncoder.encode(cipherParam, "UTF-8");
            logger.info("newApplyUrl----------------" + newApplyUrl);
            map.put("return_code", ResponseStatus.SUCCESS);
            map.put("applyUrl", newApplyUrl);
        } catch (Exception e) {
            e.printStackTrace();
            map.put("return_code", ResponseStatus.FAIL);
            map.put("return_msg", "应用异常，联系管理员");
        }
        return map;
    }


    /**
     * 检验并保存用户信息
     *
     * @param subName
     * @param subPwd
     * @param applyId
     * @param companyUUid
     * @param uuid
     */
    @RequestMapping(value = "checkAndAddUser", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> checkAndAddUser(@RequestParam(value = "subName") String subName,
                                               @RequestParam(value = "subPwd") String subPwd,
                                               @RequestParam(value = "applyId") String applyId,
                                               @RequestParam(value = "companyUUid") String companyUUid,
                                               @RequestParam(value = "userId") String uuid) {
        if (StringUtils.isEmpty(applyId) || StringUtils.isEmpty(uuid) || StringUtils.isEmpty(companyUUid)|| StringUtils.isEmpty(subName)|| StringUtils.isEmpty(subPwd)) {
           return sendBaseErrorMap(PARAMETER_FAILURE);
        }
        PortalApplyInfo applyInfo = new PortalApplyInfo();
        applyInfo.setId(Integer.valueOf(applyId));
        PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(applyInfo);
        if (null == portalApplyInfo) {
            return sendBaseErrorMap(APPLICATION_NO_EXIT);
        }

        Map<String, Object> resultMap = subAccountRuleService.chenckSubAccountAndPwd(uuid, portalApplyInfo, subName, subPwd);
        if (null == resultMap) {
            return sendBaseErrorMap(PORTAL_SYSTEM_ERROR);
        }

        Gson gson = new Gson();
        String json = gson.toJson(resultMap);
        JSONObject jsonObject = JSONObject.parseObject(json);
        Integer returnCode = (Integer) jsonObject.get("return_code");
        if (returnCode == ResponseStatus.SUCCESS) {
            return subAccountRuleService.saveSubAccount(subName, subPwd, portalApplyInfo.getClientId(), uuid);
        }
        return resultMap;
    }


    /**
     * 点击应用获取应用信息
     * 客户门户
     * @param applyId
     * @param companyUuid
     * @param
     */
    @RequestMapping(value = "/doPortalAuthorize")
    @ResponseBody
    public String doPortalAuthorize(
            @RequestParam(value = "applyId") String applyId,
            @RequestParam(value = "companyUUid") String companyUuid,
            HttpServletRequest request) {
         String uuid = (String) request.getSession().getAttribute(Constants.USERNAME);
         logger.info("uuid-----------------"+uuid);
        if (StringUtils.isEmpty(applyId) || StringUtils.isEmpty(uuid)) {
            return PARAMETER_FAILURE.getMessage();
        }
        PortalApplyInfo applyInfo = new PortalApplyInfo();
        applyInfo.setClientId(applyId);
        PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(applyInfo);
        if (null == portalApplyInfo) {
            return APPLICATION_NO_EXIT.getMessage();

        }
        if (null == portalApplyInfo.getAssociatedAccount() || portalApplyInfo.getAssociatedAccount().isEmpty()) {
            return APPLICATION_RULE_NOT_EXIT.getMessage();
        }

        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(uuid);
        if (null == userInfoDomain) {
            return USER_IS_NOT_EXIST.getMessage();
        }
        Map<String, Object> map = subAccountRuleService.getSubAccountInfo(userInfoDomain, portalApplyInfo);
        net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(map);
        int code = jsonObject.getInt("return_code");

        if (code == 0) {
            String applyUrl = jsonObject.getString("applyUrl");
            return "<script type='text/javascript'>top.location = ' " + applyUrl + " ';</script>";
        } else {
            return NO_AUTH_AUTHORIZATION.getMessage();
        }




    }


    /**
     * 获取DSG控制台跳转地址
     * 客户门户
     * @param applyId
     * @param companyUuid
     * @param
     */
    @RequestMapping(value = "doDsgConsoleAuthorize")
    @ResponseBody
    public String doDsgConsoleAuthorize(@RequestParam(value = "companyUUid") String companyUuid,
                                       HttpServletRequest request, String applyId)  {
        String uuid = (String) request.getSession().getAttribute(Constants.USERNAME);
        Map<String, Object> map = new HashMap<String, Object>();
        UserBehaviorInfo record = null;
        AppAuditInfo appAuditInfo = null;
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(uuid);
            PortalApplyInfo applyInfo = new PortalApplyInfo();
            applyInfo.setClientId(applyId);
            PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(applyInfo);
            portalApplyInfo = portalService.selectPortalInfo(portalApplyInfo);
            SubClientInfoDomain subClientInfoDomain = userInfoService.getSubNameInfo(uuid, applyId);
            if (null == subClientInfoDomain) {
                return USER_NO_SUB.getMessage();
            }
            String clientId = portalApplyInfo.getClientId();
            String account = subClientInfoDomain.getSubAccount();
            account = AES.encryptToBase64(account, AesKey.AES_KEY);
            String newApplyUrl = portalApplyInfo.getRedirectUrl() + "?userName=" + account + "&clientId=" + clientId;
        try {
            record = new UserBehaviorInfo(uuid, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), subClientInfoDomain.getSubAccount() + "登录子系统" + portalApplyInfo.getApplyName() + "成功", "登录子系统", new Date(), companyUuid);
            userBehaviorService.insertUserBehaviorInfo(record);
            String userInfo = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
            appAuditInfo = new AppAuditInfo(Integer.valueOf(portalApplyInfo.getId()),
                    userInfo, 1, subClientInfoDomain.getSubAccount() + "登录子系统" + portalApplyInfo.getApplyName() + "成功", companyUuid);
            applicationAudithService.insertAppAuditInfo(appAuditInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<script type='text/javascript'>top.location = ' " + newApplyUrl + " ';</script>";
    }




    /**
     * 管理后台跳转
     * 其他门户
     * @param
     * @param companyUUid
     */
    @RequestMapping(value = "doOtherConsoleAuthorize")
    @ResponseBody
    public String doOtherConsoleAuthorize(HttpServletRequest request, @RequestParam(value = "companyUUid") String companyUUid, HttpServletResponse response) {
        String newApplyUrl ="";
        try {
            String uuid = (String) request.getSession().getAttribute(Constants.USERNAME);
            Long timestamp = System.currentTimeMillis();
            SortedMap<String, Object> parameterMap = new TreeMap<>();
            parameterMap.put("uuid", uuid);
            parameterMap.put("companyUUid", companyUUid);
            parameterMap.put("timestamp", timestamp);
            StringBuffer sb = new StringBuffer();
            Set es = parameterMap.entrySet();  //所有参与传参的参数按照accsii排序（升序）
            Iterator it = es.iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                Object v = entry.getValue();
                //空值不传递，不参与签名组串
                if (null != v && !"".equals(v)) {
                    sb.append(v);
                }
            }
            String param = MapUtil.getMapToString(parameterMap);
            String cipherParam = AES.encryptToBase64(param, AesKey.AES_KEY);
            redisClient.put(CacheKey.getCacheKeyCipherConsoleUserInfo(uuid), cipherParam);
            SystemConfigInfo systemConfigInfo=systemConfigInfoService.getSystemConfigInfo();
            newApplyUrl = systemConfigInfo.getConsoleRedirectUrl() + "?ticket=" + URLEncoder.encode(cipherParam, "UTF-8");
            logger.info("newApplyUrl----------------" + newApplyUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return USER_NO_SUB.getMessage();
        }
        return "<script type='text/javascript'>top.location = ' " + newApplyUrl + " ';</script>";
    }







   /* *//**
     * CS应用点击事件
     * @param companyUUid
     * @param applyId
     * @param  uuid
    * *//*
    @RequestMapping(value = "/doCsAuthorize", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> doCsAuthorize(
            @RequestParam(value = "applyId") String applyId,
            @RequestParam(value = "companyUUid") String companyUUid,
            @RequestParam(value = "userId") String uuid) {

        Map<String,Object> map=new HashMap<>();
        if (StringUtils.isEmpty(applyId) ||StringUtils.isEmpty(companyUUid)||StringUtils.isEmpty(uuid)) {
              return  sendBaseErrorMap(PARAMETER_FAILURE);
        }

        PortalApplyInfo applyInfo = new PortalApplyInfo();
        applyInfo.setClientId(applyId);
        PortalApplyInfo portalApplyInfo=portalService.selectPortalInfo(new PortalApplyInfo(applyId));
        if (null == portalApplyInfo) {
            return sendBaseErrorMap(APPLICATION_NO_EXIT);

        }

        if (null == portalApplyInfo.getAssociatedAccount() || portalApplyInfo.getAssociatedAccount().isEmpty()) {
            return sendBaseErrorMap(APPLICATION_RULE_NOT_EXIT);
        }

        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(uuid);
        if (null == userInfoDomain) {
            return sendBaseErrorMap(USER_IS_NOT_EXIST);
        }

              String subName= subAccountRuleService.getSubAccountRule(userInfoDomain, portalApplyInfo.getAssociatedAccount(),applyId);

        if(StringUtils.isEmpty(subName)){
            return  sendBaseErrorMap(USER_NO_SUB);
        }

        if(StringUtils.isNotEmpty(subName)&&subName.equals(ReturnCode.SUB_RULE_ERROR)){
            return sendBaseErrorMap(SUB_NAME_RULE_ERROR);
        }

        String subPwd= subAccountRuleService.getSubPwdRule(uuid, portalApplyInfo.getAssociatedAccount(), applyId);

        if(StringUtils.isEmpty(subPwd)){
            return  sendBaseErrorMap(USER_NO_SUBPWD);
        }

        if(StringUtils.isNotEmpty(subPwd)&&subPwd.equals(ReturnCode.SUB_RULE_ERROR)){
            return  sendBaseErrorMap(SUB_NAME_RULE_ERROE);
        }
        CsApplicationInfo csApplicationInfo=new CsApplicationInfo(portalApplyInfo);
         UserPathInfo userPathInfo=new UserPathInfo();
         userPathInfo.setAppId(applyId);
         userPathInfo.setUserId(uuid);
         userPathInfo=userPathService.getUserPathInfo(userPathInfo);
         if(null!=userPathInfo&& StringUtils.isNotEmpty(userPathInfo.getPath())){
             csApplicationInfo.setPath(userPathInfo.getPath());
         }
        csApplicationInfo.getPath().replace("\\","\r");
        csApplicationInfo.setUsername(subName);
        csApplicationInfo.setPassword(subPwd);
        Gson gson=new Gson();

        String json=gson.toJson(csApplicationInfo);

        String data="";


        try {
             data= CsAppAESUtil.encode(json, CsAppAESUtil.key);
        } catch (Exception e) {
            e.printStackTrace();
        }


        map.put("data",data);
        return sendTheMap(map,SUCCESS.getCode(),SUCCESS.getMessage());
    }

*/



    /**
     * CS应用点击事件
     * @param companyUUid
     * @param applyId
     * @param  uuid
     * */
    @RequestMapping(value = "/doCsAuthorize", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> doCsAuthorize(
            @RequestParam(value = "applyId") String applyId,
            @RequestParam(value = "companyUUid") String companyUUid,
            @RequestParam(value = "userId") String uuid) {

         Map<String,Object> map=new HashMap<>();
        if (StringUtils.isEmpty(applyId) ||StringUtils.isEmpty(companyUUid)||StringUtils.isEmpty(uuid)) {
            return  sendBaseErrorMap(PARAMETER_FAILURE);
        }
        map=userPathService.getCsDate(uuid,applyId);
        return map;
    }


    /**
     * 用友Nc单点登录接口
     *
     * @param applyId     应用Id
     * @param companyUUid
     * @param uuid
     * @return
     */
    @RequestMapping(value = "/doNcAuthorize")
    @ResponseBody
    public Map<String, Object> doNcAuthorize(@RequestParam(value = "applyId") String applyId,
                                             @RequestParam(value = "companyUUid") String companyUUid,
                                             @RequestParam(value = "userId") String uuid) throws Exception {
        //入参校验
        if (StringUtils.isEmpty(applyId) || StringUtils.isEmpty(companyUUid) || StringUtils.isEmpty(uuid)) {
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用友的应用信息
        PortalApplyInfo applyInfo = new PortalApplyInfo();
        applyInfo.setClientId(applyId);
        PortalApplyInfo portalApplyInfo = portalService.selectPortalInfo(new PortalApplyInfo(applyId));
        //判断用友配置为空
        if (null == portalApplyInfo) {
            return sendBaseErrorMap(APPLICATION_NO_EXIT);

        }

        //获取portal的账号信息
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(uuid);
        //判断portal的用户是否存在
        if (null == userInfoDomain) {
            return sendBaseErrorMap(USER_IS_NOT_EXIST);
        }
        //判断账号的子账号的配置
        if (org.apache.commons.lang.StringUtils.isEmpty(portalApplyInfo.getAssociatedAccount())) {
            return sendBaseErrorMap(APPLICATION_RULE_NOT_EXIT);
        }


        //获取用友的UserCode
        String userCode = subAccountRuleService.getSubAccountRule(userInfoDomain, portalApplyInfo.getAssociatedAccount(), applyId);
        //判断userCode是否为空
        if (StringUtils.isEmpty(userCode)) {
            return sendBaseErrorMap(USER_NO_SUB);
        }

        //获取用友的密码
//        String subPwd = subAccountRuleService.getSubPwdRule(uuid, portalApplyInfo.getAssociatedAccount(), applyId);
//
//        if (StringUtils.isEmpty(subPwd)) {
//            return sendBaseErrorMap(USER_NO_SUBPWD);
//        }

        //请求用友的post请求 默认密码不开启
        List<NameValuePair> nameValuePairList = ncResService.consNcReq(portalApplyInfo, userCode);
        Map<String, Object> resMap = HttpClientUtils.doPost(portalApplyInfo.getServer(), nameValuePairList);

        //请求用友的get请求
//        String url= UrlUtils.getNcUrl(portalApplyInfo,userCode);
//        logger.info(url);
//        Map<String,Object> resMap= (Map<String, Object>) HttpClientUtils.doGet(url,"");
        //获取请求是否成功
        int resCode = (int) resMap.get("status");
        //请求失败
        if (resCode != 200) {
            return sendBaseErrorMap(ResultCode.NC_RESMAP_CODE);
        }
        //获取请求之后的返回数据信息
        String resInfo = (String) resMap.get("res");
        resInfo= resInfo.replace("\r\n","");
        //获取用友NcClient的返回路径
        String ncClientUrl = ncResService.getNcClientUrl(portalApplyInfo, resInfo, userCode);
        //获取用友的浏览器的返回路径
        //String ncClientUrl=ncResService.getNcIeUrl(portalApplyInfo,resInfo);
        //Nc返回参数构建不成功
        if (org.apache.commons.lang.StringUtils.isEmpty(ncClientUrl)) {
            return sendBaseErrorMap(ResultCode.NC_CONS_PARAMS_CODE);
        }
        //构建请求的返回参数
        Map<String, Object> map = sendBaseNormalMap();
        return setMapParam(map, "data", ncClientUrl).sendBaseNormalMap(map);
    }





    /**
     * @Comment SHA1实现
     * @Author Ron
     * @Date 2017年9月13日 下午3:30:36
     * @return
     */
    public static String shaEncode(String inStr) throws Exception {
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
            return "";
        }

        byte[] byteArray = inStr.getBytes("UTF-8");
        byte[] md5Bytes = sha.digest(byteArray);
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) {
                hexValue.append("0");
            }
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }


}
