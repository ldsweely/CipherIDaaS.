package com.portal.controller;

import com.portal.commons.ConstantsCMP;
import com.portal.publistener.UserBehaviorPublistener;
import com.portal.utils.SessionUtils;
import net.sf.json.JSONObject;
import com.cipher.china.channel.AuthResult;
import com.cipher.china.channel.factory.AuthInfoFactory;
import com.cipher.china.channel.factory.AuthMachine;
import com.cipher.china.channel.pojo.AuthInfo;
import com.cipher.china.channel.pojo.AuthStrategy;
import com.portal.commons.CacheKey;
import com.portal.commons.Constants;
import com.portal.commons.GlobalAuthType;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.listener.CookieListener;
import com.portal.redis.RedisClient;
import com.portal.service.*;
import com.portal.utils.CookieUtils;
import com.portal.utils.IpUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.portal.commons.Constants.USERNAME;
import static com.portal.enums.ResultCode.*;

/**
 * 用户绑定的业务层接口
 *
 * @author shizhao 、tiankang
 * @since 2019/4/28 13:04
 */
@RequestMapping(value = "/cipher/user")
@Controller
public class UserBindingController extends BaseController implements CookieListener {

    private static final Logger logger = LoggerFactory.getLogger(UserBindingController.class);

    @Autowired
    private RedisClient<String, Object> redisClient = new RedisClient<>();

    @Autowired
    private ObjectFactoryService objectFactoryService;

    @Autowired
    private ResetPasswordService resetPasswordService;

    @Autowired
    private UserInfoService userInfoService;


    @Autowired
    private IdentityModesService identityModesService;

    @Autowired
    private BindDingServiceInfo bindDingServiceInfo;

    @Autowired
    private PortalAuthChannelService portalAuthChannelService;

    @Autowired
    private UserLoginRecService userLoginRecService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private ISessionService sessionService;

    @Autowired
    private UserBehaviorPublistener userBehaviorPublistener;

    @Autowired
    private  WxInfoService wxInfoService;


    @PostMapping(value = "phoneNumberExist")
    @ResponseBody
    public Map<String,Object> isPhoneExist(@RequestParam(value = "userName")String userName,
                                           @RequestParam(value = "companyUUid")String companyUUid,
                                           @RequestParam(value = "phoneNumber")String phoneNumber){

        //参数校验
        if (isEmpty(userName,companyUUid,phoneNumber)){
            logger.info("There Is Empty Info Exist As userName=[{}],companyUUid=[{}],phoneNumber=[{}]",
                    new Object[]{userName,companyUUid,phoneNumber});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        UserInfoDomain userInfo = userInfoService.getCompanyUserInfoByPhoneNumber(phoneNumber,companyUUid);

        //用户不存在
        if (userInfo == null){
            return sendBaseErrorMap(USER_IS_NOT_EXIST);
        }

        return sendBaseNormalMap();
    }


    /**
     * 获取公司账号绑定的流程
     *
     * @param companyUUid 公司UUid
     * @return
     */
    @PostMapping(value = "/bindingFlow")
    @ResponseBody
    public Map<String, Object> getBindingFlow(String companyUUid) {
        Map<String, Object> map = new HashMap<>();

        //入参校验
        if (companyUUid.isEmpty()) {

            logger.warn(" enter UserBindingController.getBindingFlow  Error As The companyUUid =[{}]", new Object[]{companyUUid});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        try {

            //获取身份认证设置
            IdentitySettingInfoDomain identitySettingInfoDomain = identityModesService.getIdentityModesByCompanyUUid(companyUUid);

            //将字符串转换为实体类获取绑定流程
            AccountBinding accountBinding = null;
            if (!identitySettingInfoDomain.getBindingFlow().isEmpty()) {
                JSONObject jsonObject = JSONObject.fromObject(identitySettingInfoDomain.getBindingFlow());
                accountBinding = (AccountBinding) JSONObject.toBean(jsonObject, AccountBinding.class);

            }

            //添加到map中返回
            map.put("binDingFlow", accountBinding);


        } catch (Exception e) {

            //打印错误日志
            logger.error("Enter UserBindingController.getBindingFlow But failed companyUUid[{}]",
                    new Object[]{companyUUid});
            logger.error(e.getMessage(), e);

            sendBaseErrorMap(PORTAL_SYSTEM_ERROR);

        }

        return sendBaseNormalMap(map);

    }


    /**
     * 校验用户密码以确定要绑定的对象
     *
     * @param accountNumber 用户名
     * @param pwd           密码
     * @param companyUUid   公司的唯一ID
     * @return
     */
    @PostMapping(value = "/pwdChecked")
    @ResponseBody
    public Map<String, Object> pwdChecked(String companyUUid, String accountNumber, String pwd) {


        //入参为空校验
        if (isEmpty(companyUUid, accountNumber, pwd)) {
            logger.warn("Checked Password Error As The companyUUid =[{}] accountNumber=[{}] pwd=[{}]",
                    new Object[]{companyUUid, accountNumber, pwd});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        Map<String, Object> map = new HashMap<>();

        //获取该账号的认证方式是属于ad 还是本地
        GlobalAuthType authMode = resetPasswordService.getGlobalAuth(companyUUid, accountNumber);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreateByAuthMode(authMode);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createUserPassWordAuth(accountNumber, pwd);

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //判断认证结果
        AuthResult authResult = authMachine.getTheResult();
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取个人信息
        UserInfoDomain userInfoDomain = userInfoService.getUserInfo(accountNumber,companyUUid);
        setMapParam(map, Constants.USER_ID, userInfoDomain == null ? authResult.getUserName() : userInfoDomain.getUuid());
        return sendBaseNormalMap(map, ResultCode.SUCCESS);
    }


    /**
     * 赛赋手动绑定数据校验入口
     */
    @PostMapping(value = "/cipherBinding")
    @ResponseBody
    public Map<String, Object> saiFuManualBinding(HttpServletRequest request,HttpServletResponse response,String companyUUid, String userId, String saiFuId) {

        //入参校验
        if (isEmpty(companyUUid, userId, saiFuId)) {
            logger.warn("There is Empty Info companyUUid =[{}] userId=[{}] saiFuId=[{}] ",
                    new Object[]{companyUUid, userId, saiFuId});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户个人信息
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(userId);

        //绑定信息不存在
        if (userInfoDomain == null) {
            logger.info("User Not Found As UserId = [{}]",userId);

            return sendBaseErrorMap(ResultCode.BIND_ERROR_NOT_ACCOUNT);
        }

        //获取绑定流程
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyUUid);

        //如果绑定流程为空返回失败
        if (accountBinding == null) {
            logger.info("There Is No Evidence For User Auth..");

            return sendBaseErrorMap(ResultCode.BIND_FLOW_NOT_EXIST);
        }

        boolean complete = identityModesService.isBindingAuthComplete(userInfoDomain,accountBinding,Constants.AUTH_ON);

        if (!complete){
            logger.info("User Not Complete The Check Of Binding ..");

            return sendBaseErrorMap(BIND_FLOW_NOT_COMPLETE);
        }

        //获取账号是否已经关联
        SaiFuBindingInfoDomain saiFuBindingInfo =
                bindDingServiceInfo.getSaiFuBindingInfoBySaiFuId(saiFuId, companyUUid);

        //账号已经绑定
        if (saiFuBindingInfo != null) {
            logger.info("User Has Bind DaBay Info As PlatId =[{}]",saiFuBindingInfo.getPlatId());

            return sendBaseErrorMap(ResultCode.BINDING_EXITS_ERROR);
        }

        //构造赛赋绑定对象
        SaiFuBindingInfoDomain saiFuBindingInfoDomain1 =
                objectFactoryService.getSaiFuBindingInfoDomain(saiFuId, userInfoDomain, companyUUid);

        //绑定赛赋个人信息
        if (!bindDingServiceInfo.insertBindDingSaiFu(saiFuBindingInfoDomain1)){
            logger.info("Insert Cipher Binding Failed As Insert Catch An Exception");

            return sendBaseErrorMap(ResultCode.PORTAL_SYSTEM_ERROR);
        }

        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(userId);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, userId);

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, userId);

        //TODO:增加session,cookie
        sessionService.updateSessionExpireTime(IpUtil.getIp(),companyUUid,userId);
        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId,null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"赛赋手动绑定数据校验",companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        CookieUtils.writePortalCookie(response, userId,this::setCookies);

        Map<String, Object> map = new HashMap<>();
        setMapParam(map, Constants.USER_ID, userInfoDomain.getUuid())
                .setMapParam(map, Constants.FIRST_LOGIN, loginInfo.getFirstLogin())
                .setMapParam(map, Constants.UPDATE_PWD, loginInfo.getUpdatePwd());
        return sendBaseNormalMap(map);

    }

    /**
     * 大白手动绑定数据校验总接口
     *
     * @param companyUUid
     * @param userId
     * @param daBaiId
     * @return
     */
    @PostMapping(value = "/daBaiBinding")
    @ResponseBody
    public Map<String, Object> daBaiManualBinding(HttpServletRequest request,HttpServletResponse response,
                                                  String companyUUid, String userId, String daBaiId) {
        //入参校验
        if (isEmpty(companyUUid,userId,daBaiId)){
            logger.info("There Is Empty Info Exist As companyUUid=[{}],userId=[{}],daBaiId=[{}]",
                    new Object[]{companyUUid, userId, daBaiId});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户个人信息
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(userId);

        //绑定信息不存在
        if (userInfoDomain == null) {
            logger.info("User Not Exist As UserId = [{}]",userId);

            return sendBaseErrorMap(ResultCode.BIND_ERROR_NOT_ACCOUNT);
        }

        //获取绑定流程
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyUUid);

        //如果绑定流程为空返回失败
        if (accountBinding == null) {
            logger.info("There Is No Evidence For User Auth..");

            return sendBaseErrorMap(ResultCode.BIND_FLOW_NOT_EXIST);
        }

        //获取验证流程
        boolean complete = identityModesService.isBindingAuthComplete(userInfoDomain,accountBinding,Constants.AUTH_ON);

        if (!complete){
            logger.info("User Not Complete The Check Of Binding ..");

            return sendBaseErrorMap(BIND_FLOW_NOT_COMPLETE);
        }

        //获取账号是否已经关联
        DaBaiBindingInfoDomain dabaiInfo = bindDingServiceInfo.getDabaiBindingInfoByIdNum(daBaiId,companyUUid);

        //账号已经绑定
        if (dabaiInfo != null) {
            logger.info("User Has Bind DaBay Info As dabaiInfo =[{}]",dabaiInfo.getLinkAccount());

            return sendBaseErrorMap(ResultCode.BINDING_EXITS_ERROR);
        }

        //获取绑定对象
        DaBaiBindingInfoDomain daBaiBindingInfoDomain1 = objectFactoryService.getBindingDaBaiInfoDomain(daBaiId, userInfoDomain, companyUUid);

        //绑定大白个人信息
        if (!bindDingServiceInfo.insertDaBaiBinding(daBaiBindingInfoDomain1)){
            logger.info("Insert DaBay Binding Failed As Insert Catch An Exception");

            return sendBaseErrorMap(ResultCode.PORTAL_SYSTEM_ERROR);
        }

        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(userId);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, userId);

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, userId);

        //TODO:增加session,cookie
        sessionService.updateSessionExpireTime(IpUtil.getIp(),companyUUid,userId);
        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId,null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"大白手动绑定数据校验",companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }

        CookieUtils.writePortalCookie(response, userId,this::setCookies);

        Map<String, Object> map = new HashMap<>();

        setMapParam(map, Constants.USER_ID, userInfoDomain.getUuid())
                .setMapParam(map, Constants.FIRST_LOGIN, loginInfo.getFirstLogin())
                .setMapParam(map, Constants.UPDATE_PWD, loginInfo.getUpdatePwd());

        return sendBaseNormalMap(map);

    }


    @PostMapping(value = "/dingTalkBinding")
    @ResponseBody
    public Map<String, Object> dingTalkBinding(HttpServletRequest request, HttpServletResponse response,
                                               @RequestParam(value = "userId")String userId,
                                               @RequestParam(value = "companyUUid")String companyUUid,
                                               @RequestParam(value = "unionId")String unionId,
                                               @RequestParam(value = "openId")String openId,
                                               @RequestParam(value = "dingTalkUserId")String dingUserId) {

        //入参校验
        if (isEmpty(userId,companyUUid,unionId,openId,dingUserId)){
            logger.info("There Is Empty Info Exist As userId=[{}],companyUUid=[{}],unionId=[{}],openId=[{}],dingUserId=[{}]",
                    new Object[]{userId,companyUUid,unionId,openId,dingUserId});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户个人信息
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(userId);

        //绑定信息不存在
        if (userInfoDomain == null) {
            logger.info("User Not Found As UserId = [{}]",userId);

            return sendBaseErrorMap(ResultCode.BIND_ERROR_NOT_ACCOUNT);
        }

        //获取绑定流程
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyUUid);

        //进行完整性检查
        boolean complete = identityModesService.isBindingAuthComplete(userInfoDomain,accountBinding,Constants.AUTH_ON);

        //完整性检查失败
        if (!complete){
            logger.info("User Not Complete The Check Of Binding ..");

            return sendBaseErrorMap(BIND_FLOW_NOT_COMPLETE);
        }
        //查询本地账号是否建立建立关联
        BindingDingInfoDomain dingTalkInfo = bindDingServiceInfo.getDingTalkInfoByUnionId(unionId,companyUUid);

        //如果用户已绑定 直接返回
        if (dingTalkInfo != null) {
            logger.info("User Has Bind DingTalk Info As dingTalkInfo =[{}]",dingTalkInfo.getDingUserId());

            return sendBaseErrorMap(ResultCode.BINDING_EXITS_ERROR);
        }


        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(userId);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, userId);

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, userId);

        //TODO:增加session,cookie
        sessionService.updateSessionExpireTime(IpUtil.getIp(),companyUUid,userId);
        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId,null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"钉钉手动绑定数据检验",companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        CookieUtils.writePortalCookie(response, userId,this::setCookies);


        Map<String, Object> map = new HashMap<>();

        setMapParam(map, Constants.USER_ID, userInfoDomain.getUuid())
                .setMapParam(map, Constants.FIRST_LOGIN, loginInfo.getFirstLogin())
                .setMapParam(map, Constants.UPDATE_PWD, loginInfo.getUpdatePwd());

        dingTalkInfo = new BindingDingInfoDomain();
        dingTalkInfo.setUserId(userId);
        dingTalkInfo.setUnionId(unionId);
        dingTalkInfo.setDingUserId(dingUserId);
        dingTalkInfo.setCompanyId(companyUUid);
        //插入关联关系
        if (!bindDingServiceInfo.insertBindDing(dingTalkInfo)){
            logger.info("Insert DingTalk Binding Failed As Insert Catch An Exception");

            return sendBaseErrorMap(ResultCode.PORTAL_SYSTEM_ERROR);
        }

        return sendBaseNormalMap(map);

    }



    @PostMapping(value = "/weixinBinding")
    @ResponseBody
    public Map<String, Object> weixinBinding(HttpServletRequest request, HttpServletResponse response,
                                               @RequestParam(value = "userId")String userId,
                                               @RequestParam(value = "companyUUid")String companyUUid,
                                               @RequestParam(value = "wxUserId")String wxUserId){


        //入参校验
        if (isEmpty(userId,companyUUid,wxUserId)){
            logger.info("There Is Empty Info Exist As userId=[{}],companyUUid=[{}],wxUserId=[{}]",
                    new Object[]{userId,companyUUid,wxUserId});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户个人信息
        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByUUid(userId);

        //用户信息不存在
        if (userInfoDomain == null) {
            logger.info("User Not Found As UserId = [{}]",userId);
            return sendBaseErrorMap(ResultCode.BIND_ERROR_NOT_ACCOUNT);
        }

        //获取绑定流程
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyUUid);

        //进行完整性检查
        boolean complete = identityModesService.isBindingAuthComplete(userInfoDomain,accountBinding,Constants.AUTH_ON);

        //完整性检查失败
        if (!complete){
            logger.info("User Not Complete The Check Of Binding ..");

            return sendBaseErrorMap(BIND_FLOW_NOT_COMPLETE);
        }
        //查询本地账号是否建立建立关联
        BindWxInfoDomain bindWxInfoDomain = bindDingServiceInfo.getWxInfoByUnionId(wxUserId,companyUUid);

        //如果用户已绑定 直接返回
        if (bindWxInfoDomain != null) {
            logger.info("User Has Bind weixin Info As dingTalkInfo =[{}]",bindWxInfoDomain.getWxUserId());

            return sendBaseErrorMap(ResultCode.BINDING_EXITS_ERROR);
        }


        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(userId);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, userId);

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, userId);

        //TODO:增加session,cookie
        sessionService.updateSessionExpireTime(IpUtil.getIp(),companyUUid,userId);
        SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
        SessionUtils.setSession(request, response,USERNAME, userId);
        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId,null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"钉钉手动绑定数据检验",companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        CookieUtils.writePortalCookie(response, userId,this::setCookies);


        Map<String, Object> map = new HashMap<>();

        setMapParam(map, Constants.USER_ID, userInfoDomain.getUuid())
                .setMapParam(map, Constants.FIRST_LOGIN, loginInfo.getFirstLogin())
                .setMapParam(map, Constants.UPDATE_PWD, loginInfo.getUpdatePwd());

        bindWxInfoDomain = new BindWxInfoDomain();
        bindWxInfoDomain.setUserId(userId);
        bindWxInfoDomain.setWxUserId(wxUserId);
        bindWxInfoDomain.setCompanyId(companyUUid);
        //插入关联关系
        if (!bindDingServiceInfo.insertWeiXinInfo(bindWxInfoDomain)){
            logger.info("Insert weixin Binding Failed As Insert Catch An Exception");

            return sendBaseErrorMap(ResultCode.PORTAL_SYSTEM_ERROR);
        }

        return sendBaseNormalMap(map);

}







    /**
     * 钉钉自动绑定接口
     *
     * @param companyId
     * @param unionId
     * @return
     */
    @PostMapping(value = "/dingTalkAutoMatch")
    @ResponseBody
    public Map<String, Object> dingDingAutoBinding(@RequestParam(value = "companyUUid") String companyId,
                                                   @RequestParam(value = "unionId") String unionId,
                                                   @RequestParam(value = "phone") String phone,
                                                   @RequestParam(value = "mail") String mail) {
        //入参校验
        if (isEmpty(companyId, unionId)) {
            logger.info("There Is Empty Info As companyId = [{}],unionId = [{}]", companyId, unionId);

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取身份认证设置 获取绑定流程
        IdentitySettingInfoDomain identitySettingInfoDomain = identityModesService.getIdentityModesByCompanyUUid(companyId);

        if (identitySettingInfoDomain == null) {
            logger.info("Can't Get Any Binding Info As identitySettingInfoDomain =[null]");

            return sendBaseErrorMap(BIND_FLOW_CAN_NOT_FOUND);
        }

        //将字符串转换为实体类获取绑定流程
        if (StringUtils.isEmpty(identitySettingInfoDomain.getBindingFlow())) {
            logger.info("There Is Empty Info About Binding Flow..");

            return sendBaseErrorMap(BIND_FLOW_NOT_CONFIG);
        }

        //获取绑定信息
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyId);

        //如果自动绑定未开启 直接返回错误信息
        if (accountBinding.getBindingAppDd() != Constants.AUTH_ON || accountBinding.getAutoBindingDd() != Constants.AUTH_ON) {
            logger.info("Not Support Auto Binding..");

            return sendBaseErrorMap(NOT_BINDING_ON);
        }


        //获取钉钉配置信息
        DingdingConfigInfo dingdingConfigInfo = bindDingServiceInfo.getDingInfoByCompanyUUid(companyId);

        //配置为空返回自动绑定失败
        if (dingdingConfigInfo == null) {
            logger.info("Not Found DingDing Config...");

            return sendBaseErrorMap(BIND_FLOW_CAN_NOT_FOUND);
        }

        BindingDingInfoDomain bindingDingInfo = bindDingServiceInfo.getDingTalkInfoByUnionId(unionId, companyId);
        //钉钉绑定信息不为空
        if (bindingDingInfo != null) {
            logger.info("Binding Info Already Exist");

            return sendBaseErrorMap(REGIST_EXIST_FAILURE);
        }

        //获取匹配规则
        int rule = dingdingConfigInfo.getMatchRule();

        //根据匹配规则获取本地账号
        UserInfoDomain userInfoDomain = bindDingServiceInfo.bindLocalAccount(phone, companyId,mail, rule);


        //本地账号为空 返回绑定失败
        if (userInfoDomain == null) {
            return sendBaseErrorMap(ResultCode.AUTO_BINDING_ERROR);
        }

        Map<String, Object> map = new HashMap<>();

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userInfoDomain.getUuid(),null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"钉钉自动绑定数据检验",companyId);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }

        return setMapParam(map, Constants.USER_ID, userInfoDomain.getUuid()).sendBaseNormalMap(map, ResultCode.SUCCESS);
    }

    /**
     * 大白自动绑定
     *
     * @param idNum
     * @param companyUUid
     * @return
     */
    @PostMapping(value = "/daBaiAutoMatch")
    @ResponseBody
    public Map<String, Object> daBaiAutoBinding(@RequestParam(value = "daBaiId") String idNum,
                                                @RequestParam(value = "companyUUid") String companyUUid) {
        //参数为空校验
        if (isEmpty(companyUUid, idNum)) {
            logger.info("There Is Empty Info Exist As daBaiId = [{}] ,companyUUid = [{}]", idNum, companyUUid);

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取身份认证设置 获取绑定流程
        IdentitySettingInfoDomain identitySettingInfoDomain = identityModesService.getIdentityModesByCompanyUUid(companyUUid);

        //将字符串转换为实体类获取绑定流程
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyUUid);
        //如果自动绑定未开启 直接返回错误信息
        if (!(accountBinding.getBindingAppDb() == Constants.AUTH_ON && accountBinding.getAutoBindingDb() == Constants.AUTH_ON)) {
            logger.info("Bay Max Binding Not Allowed!");

            return sendBaseErrorMap(ResultCode.NOT_BINDING_ON);
        }

        //根据身份证号查询用户信息
        UserInfoDomain user = userInfoService.selectUserInfoByIdNum(idNum);

        //不存在 返回绑定失败
        if (user == null) {
            return sendBaseErrorMap(ResultCode.AUTO_BINDING_ERROR);
        }

        //获取账号是否已经关联
        DaBaiBindingInfoDomain daBaiBindingInfoDomain =
                bindDingServiceInfo.getDabaiBindingInfoByIdNum(idNum, companyUUid);

        //账号已经绑定
        if (daBaiBindingInfoDomain != null) {
            return sendBaseErrorMap(ResultCode.BINDING_EXITS_ERROR);
        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(),null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"大白自动绑定数据检验",companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }

        Map<String, Object> map = new HashMap<>();
        return setMapParam(map, Constants.USER_ID, user.getUuid()).sendBaseErrorMap(map, ResultCode.SUCCESS);
    }


    /**
     * 赛赋自动绑定
     *
     * @param cipherId
     * @param companyId
     * @return
     */
    @PostMapping(value = "/saiFuAutoMatch")
    @ResponseBody
    public Map<String, Object> saiFuAutoBinding(@RequestParam(value = "saiFuId") String cipherId,
                                                @RequestParam(value = "companyUUid") String companyId,
                                                @RequestParam(value = "phoneNumber") String phoneNumber,
                                                @RequestParam(value = "mail") String mail) {
        //入参校验
        if (isEmpty(cipherId, companyId)) {
            logger.info("There Is Empty Info As cipherId = [{}],companyId = [{}]", cipherId, companyId);

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取绑定流程
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyId);

        //如果自动绑定未开启 直接返回错误信息
        if (!(accountBinding.getAutoBindingSf() == Constants.AUTH_ON && accountBinding.getBindingAppSf() == Constants.AUTH_ON)) {
            logger.info("Auto Binding Not Allowed..");

            return sendBaseErrorMap(ResultCode.NOT_BINDING_ON);
        }

        //根据手机号 获取用户信息
        UserInfoDomain user = userInfoService.getUserInfoByPhone(phoneNumber);

        //手机号查询不存
        if (user == null) {
            user = userInfoService.getUserInfoByMail(mail);
        }

        //判断关联账号是否存在
        if (user == null) {
            logger.info("User Not Matched ..");

            return sendBaseErrorMap(ResultCode.AUTO_BINDING_ERROR);
        }

        //获取账号是否已经关联
        SaiFuBindingInfoDomain saiFuBindingInfoDomain =
                bindDingServiceInfo.getSaiFuBindingInfoBySaiFuId(cipherId, companyId);

        //账号已经绑定
        if (saiFuBindingInfoDomain != null) {
            logger.info("Matched User Has Binding An Cipher Account..");

            return sendBaseErrorMap(ResultCode.BINDING_EXITS_ERROR);
        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(),null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"赛赋自动绑定数据检验",companyId);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Object> map = new HashMap<>();
        return setMapParam(map, Constants.USER_ID, user.getUuid()).sendBaseErrorMap(map, ResultCode.SUCCESS);

    }








    /**
     * 微信自动绑定接口
     *
     * @param companyId
     * @param wxUserId
     * @return
     */
    @PostMapping(value = "/weixinAutoMatch")
    @ResponseBody
    public Map<String, Object> weixinAutoMatch(@RequestParam(value = "companyUUid") String companyId,
                                                   @RequestParam(value = "wxUserId") String wxUserId,
                                                   @RequestParam(value = "phone") String phone,
                                                   @RequestParam(value = "mail") String mail) {
        //入参校验
        if (isEmpty(companyId, wxUserId)) {
            logger.info("There Is Empty Info As companyId = [{}],wxUserId = [{}]", companyId, wxUserId);

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取身份认证设置 获取绑定流程
        IdentitySettingInfoDomain identitySettingInfoDomain = identityModesService.getIdentityModesByCompanyUUid(companyId);

        if (identitySettingInfoDomain == null) {
            logger.info("Can't Get Any Binding Info As identitySettingInfoDomain =[null]");

            return sendBaseErrorMap(BIND_FLOW_CAN_NOT_FOUND);
        }

        //将字符串转换为实体类获取绑定流程
        if (StringUtils.isEmpty(identitySettingInfoDomain.getBindingFlow())) {
            logger.info("There Is Empty Info About Binding Flow..");

            return sendBaseErrorMap(BIND_FLOW_NOT_CONFIG);
        }

        //获取绑定信息
        AccountBinding accountBinding = identityModesService.getAccountBindingByCompanyId(companyId);

        //如果自动绑定未开启 直接返回错误信息
        if (accountBinding.getBindingAppDd() != Constants.AUTH_ON || accountBinding.getAutoBindingDd() != Constants.AUTH_ON) {
            logger.info("Not Support Auto Binding..");

            return sendBaseErrorMap(NOT_BINDING_ON);
        }


        //获取钉钉配置信息
        WeiXinConfig weiXinConfig = wxInfoService.getWeiXinConfigInfo(companyId);

        //配置为空返回自动绑定失败
        if (weiXinConfig == null) {
            logger.info("Not Found DingDing Config...");

            return sendBaseErrorMap(BIND_FLOW_CAN_NOT_FOUND);
        }

        BindWxInfoDomain bindWxInfoDomain = bindDingServiceInfo.getWxInfoByUnionId(wxUserId, companyId);
        //钉钉绑定信息不为空
        if (bindWxInfoDomain != null) {
            logger.info("Binding Info Already Exist");

            return sendBaseErrorMap(REGIST_EXIST_FAILURE);
        }

        //获取匹配规则
        int rule = weiXinConfig.getMatchRule();

        //根据匹配规则获取本地账号
        UserInfoDomain userInfoDomain = bindDingServiceInfo.bindLocalAccount(phone, companyId,mail, rule);


        //本地账号为空 返回绑定失败
        if (userInfoDomain == null) {
            return sendBaseErrorMap(ResultCode.AUTO_BINDING_ERROR);
        }

        Map<String, Object> map = new HashMap<>();

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userInfoDomain.getUuid(),null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN,IpUtil.getIp(),"钉钉自动绑定数据检验",companyId);
            userBehaviorPublistener.publish(userBehaviorInfo);
        }catch (Exception e){
            e.printStackTrace();
        }

        return setMapParam(map, Constants.USER_ID, userInfoDomain.getUuid()).sendBaseNormalMap(map, ResultCode.SUCCESS);
    }




    @Override
    public void setCookies(String key, String value) {
        redisClient.put(key,value);
    }
}
