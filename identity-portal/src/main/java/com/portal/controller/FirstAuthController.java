package com.portal.controller;




import com.alibaba.fastjson.JSON;
import com.cipher.china.channel.AuthResult;
import com.cipher.china.channel.factory.AuthInfoFactory;
import com.cipher.china.channel.factory.AuthMachine;
import com.cipher.china.channel.pojo.*;
import com.google.gson.Gson;
import com.portal.commons.*;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.enums.UserStateEnum;
import com.portal.event.CasServerRedirec;
import com.portal.event.CasServerRedirectEvent;
import com.portal.listener.*;
import com.portal.publistener.UserBehaviorPublistener;
import com.portal.redis.RedisClient;
import com.portal.service.*;
import com.portal.token.PassToken;
import com.portal.utils.CookieUtils;
import com.portal.utils.IpUtil;
import com.portal.utils.ResponseUtils;
import com.portal.utils.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.portal.commons.Constants.*;
import static com.portal.enums.ResultCode.*;

/**
 * 首次认证的业务层
 * create by shizhao at 2019/5/27
 *
 * @author shizhao
 * @since 2019/5/17
 */
@RequestMapping(value = "/cipher/user")
@Controller
public class FirstAuthController extends BaseController implements CookieListener {

    @Autowired
    private CasAndRdsgConfigService casAndRdsgConfigService;

    @Autowired
    private ResponseRedirectService responseRedirectService;

    @Autowired
    private PortalService portalService;

    @Autowired
    private SubAccountRuleService subAccountRuleService;

    @Autowired
    private UserBehaviorPublistener userBehaviorPublistener;

    private static final Logger logger = LoggerFactory.getLogger(FirstAuthController.class);

    @Autowired
    private PortalAuthChannelService portalAuthChannelService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private IdentityModesService identityModesService;

    @Autowired
    private ObjectFactoryService objectFactoryService;

    @Autowired
    private ResetPasswordService resetPasswordService;

    @Autowired
    private UserLoginRecService userLoginRecService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private PortalToXDsgService portalToXDsgService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JudgeLimit judgeLimit;
    @Autowired
    private ISessionService sessionService;

    @Autowired
    private LoginFailedUserService loginFailedUserService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SsoLoginService ssoLoginService;

    @Autowired
    private RedisClient<String, Object> redisClient = new RedisClient<>();

    /**
     * 用户名密码登录接口
     *
     * @param companyUUid   公司ID
     * @param accountNumber 用户登录名
     * @param pwd           用户密码
     */
    @PassToken
    @RequestMapping(value = "/pwdLoginChecked")
    @ResponseBody
    public Map<String, Object> pwdLogin(HttpServletRequest request, HttpServletResponse response, String companyUUid, String accountNumber, String pwd) {
        //入参校验
        if (isEmpty(companyUUid, accountNumber, pwd)) {
            logger.warn(" Check Info Failed As The companyUUid =[{}] accountNumber=[{}] pwd=[{}]"
                    , new Object[]{companyUUid, accountNumber, pwd});

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

     /*   if (!accountNumber.contains("@")) {
            accountNumber += "@unittec.com";
        }*/


        Map<String, Object> map = new HashMap<>();

        //获取系统身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //获取认证方式失败
        if (identityAuthentication == null) {
            logger.info("Identity Config Is Empty");

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }



        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfo(accountNumber,companyUUid);

        String uuid = user == null ? null : user.getUuid();

        IdentityAuthTypeDomain identityAuthTypeDomain = groupService.queryAuthTypeByUserId(uuid, companyUUid);

        identityAuthTypeDomain = identityAuthTypeDomain == null?groupService.queryDefaultAuthTypeByCompanyId(companyUUid):identityAuthTypeDomain;
        //存在空的配置信息
        if (identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist");

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //获取该账号的认证方式是属于ad 还是本地
        GlobalAuthType authMode = resetPasswordService.getGlobalAuthByIdentity(identityAuthTypeDomain);

        //生成认证策略
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication, authMode);
        //authStrategy.setLocalAuth(AuthStrategy.ON);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createUserPassWordAuth(accountNumber, pwd);

        //判断用户的状态
        UserStateEnum userStateMsg = userInfoService.getUserState(user);

        if (null != userStateMsg && userStateMsg.getCode() == 1203) {
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(), null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "账号被锁定",  companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }


        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //获得认证结果
        AuthResult authResult = authMachine.getTheResult();

        if (!authMachine.isSuccess()) {
            logger.info("Auth Failed As The Message = [{}]", authResult.getResultMessage());
            //TODO 存入缓存次数

            loginFailedUserService.loginFailedValidate(uuid, IpUtil.getIp());
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());

        }

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo = userLoginRecService.selectUserLoginRecInfoByUUid(uuid);

        //构造用户的登录信息
        LoginInfo loginInfo = loginService.getLoginInfo(userLoginRecInfo, identityAuthentication, identityAuthTypeDomain,
                uuid == null ? authResult.getUserName() : uuid);
//
        SecondLoginInfo secondLoginInfo = loginInfo.needSecondAuth() ?
                resetPasswordService.getSecondLoginInfo(companyUUid, uuid) : null;

        //设置二次认证信息
        setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo);

        if (identityModesService.isAuthComplete(null, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, uuid);
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request,response, USERNAME, user.getUuid());
        }
        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(), null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "登入portal成功", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("enter FirstAuthController.pwdLogin() an exception occurs");
        }

        return setMapParam(map, Constants.LOGIN_INFO, loginInfo)
                .setMapParam(map, Constants.IS_BOUND, Constants.AUTH_ON)
                .sendBaseNormalMap(map);

    }



    /**
     * TOTP和账号密码登录接口
     *
     * @param companyUUid   公司ID
     * @param accountNumber 用户登录名
     * @param totp          totp验证码
     */
    @RequestMapping(value = "/pwdTotpLoginChecked")
    @ResponseBody
    public Map<String, Object> pwdTotpLogin(HttpServletRequest request, HttpServletResponse response, String companyUUid, String accountNumber, String totp) {

        //参数空值校验
        if (isEmpty(companyUUid, accountNumber, totp)) {
            logger.warn(" enter OnceLoginController.pwdTotpLogin " +
                            "Error As The companyUUid =[{}] accountNumber=[{}] ",
                    new Object[]{companyUUid, accountNumber});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        Map<String, Object> map = new HashMap<>();

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfo(accountNumber,companyUUid);

        if (user == null) {
            logger.info("This User =[{}] Is Not Register", accountNumber);

            return sendBaseErrorMap(LOGIN_PHONE_NOT_REGISTER);
        }

        //获得用户的uuid
        String uuid = user.getUuid();


        //判断用户的状态
        UserStateEnum userStateMsg = userInfoService.getUserState(uuid);
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }

        //获取用户的属于哪个身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = groupService.queryAuthTypeByUserId(uuid, companyUUid);
        identityAuthTypeDomain = identityAuthTypeDomain == null ? groupService.queryDefaultAuthTypeByCompanyId(companyUUid) : identityAuthTypeDomain;


        //获取该账号的认证方式是属于ad 还是本地
        GlobalAuthType auth_mode = resetPasswordService.getGlobalAuthByIdentity(identityAuthTypeDomain);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication, auth_mode);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createTotpAuth(uuid, totp);

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //认证失败
        if (!authMachine.isSuccess()) {

            AuthResult authResult = authMachine.getTheFailedResult();
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(uuid);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo = loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, uuid);

        //设置二次认证信息
        SecondLoginInfo secondLoginInfo = null;
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, uuid);
            secondLoginInfo.setTwoAuthDt(Constants.AUTH_OFF);
        }

        //判断二次认证是串行还是并行 true：并行 false:串行

        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(uuid));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setSaiFuTotpLocalUuid(uuid);
        redisClient.put(CacheKey.getCacheKeyUserAuth(uuid), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, uuid);
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            //TODO
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request, response,Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request,response, USERNAME, user.getUuid());
        }


        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(), null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "赛赋totp动态码认证成功", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo)
                .setMapParam(map, Constants.LOGIN_INFO, loginInfo)
                .setMapParam(map, Constants.IS_BOUND, Constants.AUTH_ON)
                .sendBaseNormalMap(map);
    }


    /**
     * 赛赋扫码认证轮询接口
     *
     * @param uuid        二维码唯一标识
     * @param companyUUid 公司ID
     */
    @PostMapping(value = "/saiFuPolling")
    @ResponseBody
    public Map<String, Object> getSaiFuResult(HttpServletRequest request, HttpServletResponse response, String uuid, String companyUUid) {

        //入参校验
        if (isEmpty(uuid, companyUUid)) {
            logger.warn(" enter OnceLoginController.getSaiFuResult  Error As The companyUUid =[{}] uuid=[{}]  ",
                    new Object[]{companyUUid, uuid});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        Map<String, Object> map = new HashMap<>();

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createCipherScanAuth(uuid);

        CipherAuthListener cipherAuthListener = new CipherAuthListener();

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid, cipherAuthListener);

        //对结果进行校验 如果验证失败 直接返回结果
        AuthResult authResult = authMachine.getTheResult();
        if (!authMachine.isSuccess()) {
            logger.info("Auth Failed As Message=[{}]", authResult.getResultMessage());

            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取扫码用户信息
        String userPlantId = authResult.getUserName();

        //查找用户与赛赋扫码是否绑定
        UserInfoDomain user = userInfoService.getUser(userPlantId, companyUUid);

        //设置扫码绑定信息和赛赋信息
        setMapParam(map, Constants.SAI_FU_BOUND, user == null ? Constants.AUTH_ERROR : Constants.AUTH_ON);

        CipherScanAuthUser authUser = new Gson().fromJson(authResult.getResult(), CipherScanAuthUser.class);

        //user为空  未绑定
        if (user == null) {
            setMapParam(map, Constants.SAI_FU_INFO, authUser);
            return sendBaseNormalMap(map);
        }


        //判断用户的状态、停用、锁定、不存在
        UserStateEnum userStateMsg = userInfoService.getUserState(user.getUuid());
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }


        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(user.getUuid());

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain =
                resetPasswordService.getIdentityAuthType(companyUUid, user.getUuid());

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, user.getUuid());

        //二次认证信息
        SecondLoginInfo secondLoginInfo = null;
        //设置二次认证信息
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, user.getUuid());
            secondLoginInfo.setTwoAuthSf(Constants.AUTH_OFF);
        }

        //判断二次认证是串行还是并行 true：并行 false:串行

        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setSaiFuBindLocalUuid(user.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, user.getUuid());
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request, response,USERNAME, user.getUuid());

        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(), null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "赛赋扫码认证成功", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo).
                setMapParam(map, Constants.LOGIN_INFO, loginInfo).
                sendBaseNormalMap(map);
    }


    /**
     * 大白扫码认证轮询接口
     *
     * @param uuid        二维码唯一标识
     * @param companyUUid 公司ID
     */
    @RequestMapping(value = "/daBaiPolling")
    @ResponseBody
    public Map<String, Object> getDabaiResult(HttpServletRequest request, HttpServletResponse response, String uuid, String companyUUid) {

        //入参校验
        if (isEmpty(uuid, companyUUid)) {

            logger.warn(" enter OnceLoginController.getDabaiResult  Error As The companyUUid =[{}] uuid=[{}]  ",
                    new Object[]{companyUUid, uuid});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        Map<String, Object> map = new HashMap<>();

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createBayMaxScanAuth(uuid);

        //大白扫码结果获取监听器
        BayMaxAuthListener bayMaxAuthListener = new BayMaxAuthListener();

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid, bayMaxAuthListener);

        AuthResult authResult = authMachine.getTheResult();

        //对结果进行校验
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取扫码用户信息
        String userPlantId = authResult.getUserName();

        //查找用户与大白扫码是否绑定
        UserInfoDomain user = userInfoService.getUserByDabai(userPlantId, companyUUid);

        BayMaxScanAuthUser authUser = new Gson().fromJson(authResult.getResult(), BayMaxScanAuthUser.class);

        //设置绑定信息
        setMapParam(map, Constants.DA_BAI_BOUND, user == null ? Constants.AUTH_ERROR : Constants.AUTH_ON)
                .setMapParam(map, Constants.DA_BAI_ID, userPlantId);
        //未绑定
        if (user == null) {
            setMapParam(map, Constants.DA_BAI_INFO, authUser);
            return sendBaseNormalMap(map);
        }

        //判断用户的状态、停用、锁定、不存在
        UserStateEnum userStateMsg = userInfoService.getUserState(user.getUuid());
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }


        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(user.getUuid());

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain =
                resetPasswordService.getIdentityAuthType(companyUUid, user.getUuid());

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, user.getUuid());

        //二次认证信息
        SecondLoginInfo secondLoginInfo = null;
        //设置二次认证信息
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, user.getUuid());
            secondLoginInfo.setTwoAuthDb(Constants.AUTH_OFF);
        }


        //判断二次认证是串行还是并行 true：并行 false:串行

        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setDaBaiBindLocalUuid(user.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, user.getUuid());
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            //TODO
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request, response,Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request, response,USERNAME, user.getUuid());

        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(user.getUuid(), null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "大白扫码认证成功", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo).
                setMapParam(map, Constants.LOGIN_INFO, loginInfo).
                sendBaseNormalMap(map);

    }

    /**
     * 钉钉扫码认证轮询接口
     *
     * @param code        钉钉返回的唯一标识
     * @param companyUUid 公司ID
     */
    @RequestMapping(value = "/dingTalkLogin")
    @ResponseBody
    public Map<String, Object> getDingDingResult(HttpServletRequest request, HttpServletResponse response,
                                                 @RequestParam(value = "code") String code,
                                                 @RequestParam(value = "companyUUid") String companyUUid) {
        //钉钉标识码空校验
        if (isEmpty(code)) {
            logger.info("Ding Talk Code Is Empty Code= [{}]", code);
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createDingTalkScanAuth(code);

        DingTalkAuthListener dingTalkAuthListener = new DingTalkAuthListener();

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid, dingTalkAuthListener);

        AuthResult authResult = authMachine.getTheResult();
        //对结果进行校验 如果验证失败 直接返回结果
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取扫码用户信息
        String userPlantId = authResult.getUserName();

        //查找用户与钉钉扫码是否绑定
        UserInfoDomain user = userInfoService.selectUserInfoByDingTalkInfo(companyUUid, userPlantId);

        Map<String, Object> map = new HashMap<>();

        setMapParam(map, Constants.DING_TALK_BOUND, user == null ? Constants.AUTH_ERROR : Constants.AUTH_ON)
                .setMapParam(map, Constants.DING_TALK_ID, userPlantId);

        DingTalkScanAuthUser authUser = new Gson().fromJson(authResult.getResult(), DingTalkScanAuthUser.class);
        //user为空  未绑定
        if (user == null) {
            setMapParam(map, Constants.DING_TALK_INFO, authUser.getUserInfo());
            return sendBaseNormalMap(map);
        }

        String uuid = user.getUuid();

        //判断用户的状态
        UserStateEnum userStateMsg = userInfoService.getUserState(uuid);
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }

        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(uuid);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain =
                resetPasswordService.getIdentityAuthType(companyUUid, uuid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, uuid);

        //二次认证信息
        SecondLoginInfo secondLoginInfo = null;
        //设置二次认证信息
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, uuid);

            secondLoginInfo.setTwoAuthDd(Constants.AUTH_OFF);
        }


        //判断二次认证是串行还是并行 true：并行 false:串行

        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(uuid));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setDingBindLocalUuid(uuid);
        redisClient.put(CacheKey.getCacheKeyUserAuth(uuid), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, uuid);
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request, response,USERNAME, user.getUuid());
        }

        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo).
                setMapParam(map, Constants.LOGIN_INFO, loginInfo).
                sendBaseNormalMap(map);

    }



    /**
     * 微信扫码认证轮询接口
     *
     * @param code        微信返回的唯一标识
     * @param companyUUid 公司ID
     */
    @RequestMapping(value = "/weixinPolling")
    @ResponseBody
    public Map<String, Object> getWeiXinResult(HttpServletRequest request, HttpServletResponse response,
                                                 @RequestParam(value = "code") String code,
                                                 @RequestParam(value = "companyUUid") String companyUUid) {
        //钉钉标识码空校验
        if (isEmpty(code)) {
            logger.info("WeiXin Code Is Empty Code= [{}]", code);
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createWeixinAuth(code);


        WeixinAuthListener weixinAuthListener = new WeixinAuthListener();

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid, weixinAuthListener);


        AuthResult authResult = authMachine.getTheResult();
        logger.info("weixin auth result info "+authResult);
        //对结果进行校验 如果验证失败 直接返回结果
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取扫码用户信息
        String userWeixinId = authResult.getUserName();

        //查找用户与w微信扫码是否绑定
        UserInfoDomain user = userInfoService.selectUserInfoByWeixinInfo(userWeixinId,companyUUid);


        Map<String, Object> map = new HashMap<>();

        setMapParam(map, Constants.WEI_XIN_BOUND, user == null ? Constants.AUTH_ERROR : Constants.AUTH_ON)
                .setMapParam(map, Constants.WX_UNION_ID, userWeixinId);

        WeixinScanAuthUser authUser = new Gson().fromJson(authResult.getResult(), WeixinScanAuthUser.class);
        //user为空  未绑定
        if (user == null) {
            setMapParam(map, Constants.WX_INFO, authUser);
            return sendBaseNormalMap(map);
        }

        String uuid = user.getUuid();

        //判断用户的状态
        UserStateEnum userStateMsg = userInfoService.getUserState(uuid);
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }

        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(uuid);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain =
                resetPasswordService.getIdentityAuthType(companyUUid, uuid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo =
                loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, uuid);

        //二次认证信息
        SecondLoginInfo secondLoginInfo = null;
        //设置二次认证信息
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, uuid);
            secondLoginInfo.setTwoAuthWx(Constants.AUTH_OFF);
        }

        //判断二次认证是串行还是并行 true：并行 false:串行
        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(uuid));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setWeixinLocalUuid(uuid);
        redisClient.put(CacheKey.getCacheKeyUserAuth(uuid), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, uuid);
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request, response,USERNAME, user.getUuid());
        }

        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo).
                setMapParam(map, Constants.LOGIN_INFO, loginInfo).
                sendBaseNormalMap(map);

    }


    /**
     * 手机随机码登录接口
     *
     * @param companyUUid 公司ID
     * @param phone       用户的手机号
     * @param code        用户的随机码
     */
    @RequestMapping(value = "/phoneCodeLogin")
    @ResponseBody
    public Map<String, Object> getPhoneCodeResult(HttpServletRequest request, HttpServletResponse response,
                                                  @RequestParam(value = "companyUUid") String companyUUid,
                                                  @RequestParam(value = "phoneNumber") String phone,
                                                  @RequestParam(value = "code") String code) {

        //参数为空校验
        if (isEmpty(companyUUid, phone, code)) {
            logger.warn("Check Info failed As There Are Empty Info Exist..");

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        UserInfoDomain userInfoDomain = userInfoService.getUserInfoByPhone(phone);

        //用户不存在
        if (userInfoDomain == null) {
            logger.info("This PhoneNumber =[{}] Is Not Register");

            return sendBaseErrorMap(LOGIN_PHONE_NOT_REGISTER);
        }

        String uuid = userInfoDomain.getUuid();

        //判断用户的状态
        UserStateEnum userStateMsg = userInfoService.getUserState(uuid);
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        if (identityAuthentication == null) {
            logger.info("Identity Config Is Empty");

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //获取该账号的认证方式是属于ad 还是本地
        GlobalAuthType authMode = resetPasswordService.getGlobalAuth(companyUUid, uuid);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication, authMode);

        //创建认证对象
        AuthInfo authInfo = AuthInfoFactory.createSmsCodeAuth(phone, code);

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //获得认证结果
        AuthResult authResult = authMachine.getTheResult();

        //认证失败
        if (!authResult.isSuccess()) {
            logger.info("Auth Failed As The Message = [{}]", authResult.getResultMessage());

            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo = userLoginRecService.selectUserLoginRecInfoByUUid(uuid);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, uuid);

        //存在空的配置信息
        if (identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }
        //构造用户的登录信息
        LoginInfo loginInfo = loginService.getLoginInfo(userLoginRecInfo, identityAuthentication, identityAuthTypeDomain, uuid);

        Map<String, Object> map = new HashMap<>();
        //二次认证信息
        SecondLoginInfo secondLoginInfo = null;
        //设置二次认证信息
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, uuid);
            secondLoginInfo.setTwoAuthNum(Constants.AUTH_OFF);
        }

        //判断二次认证是串行还是并行 true：并行 false:串行

        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }



        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(uuid));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setPhoneNumber(AuthFlow.SUCCESS);
        redisClient.put(CacheKey.getCacheKeyUserAuth(uuid), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, uuid);
            CookieUtils.writePortalCookie(response, uuid, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(uuid));
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request,response, USERNAME, uuid);
        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(uuid, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "手机验证码认证成功", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo).
                setMapParam(map, Constants.LOGIN_INFO, loginInfo).
                setMapParam(map, Constants.IS_BOUND, Constants.AUTH_ON).
                sendBaseNormalMap(map);
    }


    /**
     * 手机信息采集接口
     *
     * @param companyUUid 公司ID
     * @param phone       用户的手机号
     * @param code        用户的随机码
     * @param userId      用户的唯一标识
     */
    @PostMapping(value = "/collectPhoneNumber")
    @ResponseBody
    public Map<String, Object> registerPhoneNumber(HttpServletRequest request, HttpServletResponse response,
                                                   @RequestParam(value = "companyUUid") String companyUUid,
                                                   @RequestParam(value = "userId") String userId,
                                                   @RequestParam(value = "phone") String phone,
                                                   @RequestParam(value = "code") String code) {
        //参数为空校验
        if (isEmpty(companyUUid, phone, code, userId)) {
            logger.warn("Check Info failed but param companyUuid=[{}] code = [{}]", companyUUid, code);

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        UserInfoDomain userInfoOfUserId = userInfoService.getUserInfoByUUid(userId);
        //用户不存在
        if (userInfoOfUserId == null) {
            logger.info("Can't Accept The User As The User Of UserId = [{}] Is Not Exist.", userId);

            return sendBaseErrorMap(LOGIN_COLLECT_USER_NOT_EXIST);
        }

        //用户已经绑定了一个手机号
        if (StringUtils.isNotEmpty(userInfoOfUserId.getPhoneNumber())) {
            logger.info("This User = [{}] Has Register Phone!", userId);

            return sendBaseErrorMap(LOGIN_COLLECT_USER_HAS_REGISTER_PHONE);
        }

        UserInfoDomain userInfoOfPhone = userInfoService.getUserInfoByPhone(phone);
        //该手机号已经绑定用户
        if (userInfoOfPhone != null) {
            logger.info("This Phone = [{}] Has Been Register", phone);

            return sendBaseErrorMap(LOGIN_COLLECT_USER_PHONE_HAS_BEEN_REGISTER);
        }

        //与系统策略无关
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setPhoneRandomCode(AuthStrategy.ON);
        //创建认证信息
        AuthInfo authInfo = AuthInfoFactory.createSmsCodeAuth(phone, code);
        //去认证中心认证
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();
        //认证失败
        if (!authMachine.isSuccess()) {
            logger.info("Auth Failed The Message = [{}]", authResult.getResultMessage());

            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        userInfoOfUserId.setPhoneNumber(phone);
        //更新失败
        if (!userInfoService.updateUserPhone(userInfoOfUserId)) {
            logger.info("Update Failed Because Caught An Exception!");

            return sendBaseErrorMap(LOGIN_COLLECT_UPDATE_FAILED);
        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "用户手机号采集", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sendBaseNormalMap();
    }


    /**
     * 邮箱信息采集接口
     *
     * @param companyUUid 公司ID
     * @param mail        用户的邮箱号
     * @param code        用户的随机码
     * @param userId      用户的唯一标识
     */
    @PostMapping(value = "/collectMailNumber")
    @ResponseBody
    public Map<String, Object> registerMailNumber(HttpServletRequest request, HttpServletResponse response,
                                                  @RequestParam(value = "companyUUid") String companyUUid,
                                                  @RequestParam(value = "userId") String userId,
                                                  @RequestParam(value = "mail") String mail,
                                                  @RequestParam(value = "code") String code) {
        //参数为空校验
        if (isEmpty(companyUUid, mail, code, userId)) {
            logger.warn("Check Info failed but param companyUuid=[{}] code = [{}]", companyUUid, code);

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        UserInfoDomain userInfoOfUserId = userInfoService.getUserInfoByUUid(userId);
        //用户不存在
        if (userInfoOfUserId == null) {
            logger.info("Can't Accept The User As The User Of UserId = [{}] Is Not Exist.", userId);

            return sendBaseErrorMap(LOGIN_COLLECT_USER_NOT_EXIST);
        }

        //用户已经绑定了一个邮箱
        if (StringUtils.isNotEmpty(userInfoOfUserId.getMail())) {
            logger.info("This User = [{}] Has Register mail!", userId);

            return sendBaseErrorMap(LOGIN_COLLECT_USER_HAS_REGISTER_MAIL);
        }

        UserInfoDomain userInfoOfMail = userInfoService.getUserInfoByMail(mail);
        //该邮箱已经绑定用户
        if (userInfoOfMail != null) {
            logger.info("This mail = [{}] Has Been Register", mail);

            return sendBaseErrorMap(LOGIN_COLLECT_USER_MAIL_HAS_BEEN_REGISTER);
        }

        //与系统策略无关
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setMailRandomCode(AuthStrategy.ON);
        //创建认证信息
        AuthInfo authInfo = AuthInfoFactory.createMailCodeAuth(mail, code);
        //去认证中心认证
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();
        //认证失败
        if (!authMachine.isSuccess()) {
            logger.info("Auth Failed The Message = [{}]", authResult.getResultMessage());

            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        userInfoOfUserId.setMail(mail);
        //更新失败
        if (!userInfoService.updateUserMail(userInfoOfUserId)) {
            logger.info("Update Failed Because Caught An Exception!");

            return sendBaseErrorMap(LOGIN_COLLECT_UPDATE_FAILED);
        }

        try {
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "用户邮箱采集", companyUUid);
            userBehaviorPublistener.publish(userBehaviorInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sendBaseNormalMap();
    }





    /**
     * otp动态码账号密码登录接口
     *
     * @param companyUUid   公司ID
     * @param accountNumber 用户登录名
     * @param otpDynamicCode      otp验证码
     */
    @RequestMapping(value = "/otpLoginChecked")
    @ResponseBody
    public Map<String, Object> otpLogin(HttpServletRequest request, HttpServletResponse response, String companyUUid, String accountNumber, String otpDynamicCode) {

        //参数空值校验
        if (isEmpty(companyUUid, accountNumber, otpDynamicCode)) {
            logger.warn(" enter FirstAuthController.dingTotpLogin " +
                            "Error As The companyUUid =[{}] accountNumber=[{}] otpDynamicCode=[{}] ",
                    new Object[]{companyUUid, accountNumber,otpDynamicCode});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        Map<String, Object> map = new HashMap<>();

        //获取身份认证方式
        IdentityAuthentication identityAuthentication =
                identityModesService.getIdentityAuthenticationByCompanyId(companyUUid);

        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfo(accountNumber,companyUUid);

        if (user == null) {
            logger.info("This User =[{}] Is Not Register", accountNumber);

            return sendBaseErrorMap(LOGIN_PHONE_NOT_REGISTER);
        }

        //获得用户的uuid
        String uuid = user.getUuid();


        //判断用户的状态
        UserStateEnum userStateMsg = userInfoService.getUserState(uuid);
        if (null != userStateMsg) {
            return sendTheMap(userStateMsg.getCode(), userStateMsg.getDesc());
        }

        //获取该账号的认证方式是属于ad 还是本地
        GlobalAuthType auth_mode = resetPasswordService.getGlobalAuth(companyUUid, uuid);

        //根据认证策略创建认证中心对象
        AuthStrategy authStrategy = objectFactoryService.authStrategyCreate(identityAuthentication, auth_mode);

        //创建认证内容的对象
        AuthInfo authInfo = AuthInfoFactory.createOtpDynamicAuth(uuid, otpDynamicCode);
        authStrategy.setOtpDynamicCode(Constants.ON);

        //去认证中心认证 返回认证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //认证失败
        if (!authMachine.isSuccess()) {
            AuthResult authResult = authMachine.getTheFailedResult();
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //创建首次登陆对象
        UserLoginRecInfo userLoginRecInfo = objectFactoryService.userLoginRecServiceCreate(uuid);

        //获取是否首次登陆
        UserLoginRecInfo userLoginRecInfo1 = userLoginRecService.selectUserLoginRecInfo(userLoginRecInfo);

        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, uuid);

        //存在空的配置信息
        if (identityAuthentication == null || identityAuthTypeDomain == null) {
            logger.info("There is Empty Config Exist In " +
                            "identityAuthentication[{}], identityAuthTypeDomain=[{}]",
                    identityAuthentication, identityAuthTypeDomain);

            return sendBaseErrorMap(LOGIN_CONFIG_NOT_COMPLETE);
        }

        //构造用户的登录信息
        LoginInfo loginInfo = loginService.getLoginInfo(userLoginRecInfo1, identityAuthentication, identityAuthTypeDomain, uuid);

        SecondLoginInfo secondLoginInfo=null;
        //设置二次认证信息
        if (loginInfo.needSecondAuth()) {
            secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, uuid);
            secondLoginInfo.setTwoAuthDt(Constants.AUTH_OFF);
        }

        //判断二次认证是串行还是并行 true：并行 false:串行

        if(loginInfo.isParallel()){
            secondLoginInfo=null;
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(uuid));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }
        authFlow.setSaiFuTotpLocalUuid(uuid);
        redisClient.put(CacheKey.getCacheKeyUserAuth(uuid), authFlow, EXPIRES);

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(),companyUUid,uuid);
            CookieUtils.writePortalCookie(response, user.getUuid(), this::setCookies);
            //TODO
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
            SessionUtils.setSession(request, response,USERNAME, user.getUuid());
        }


        return setMapParam(map, Constants.SECOND_LOGIN_INFO, secondLoginInfo)
                .setMapParam(map, Constants.LOGIN_INFO, loginInfo)
                .setMapParam(map, Constants.IS_BOUND, Constants.AUTH_ON)
                .sendBaseNormalMap(map);
    }






    /**
     * 获取走正常登陆还是跳转到其他应用接口
     *
     * @param request
     * @param response
     */
    @RequestMapping(value = "/flowsuccess")
    public void flowSuccess(HttpServletRequest request, HttpServletResponse response, String companyUuid) {

        //获取rdsg以及cas的认证界面地址配置
        String authUrl=casAndRdsgConfigService.getCasServerAuthUrl();
        String rDsgUrl=casAndRdsgConfigService.getRdsgServerUrl();

        String serviceUrl = (String) SessionUtils.getSessionByName(request, Constants.SERVICEURL);

        if (org.apache.commons.lang.StringUtils.isEmpty(serviceUrl)) {
            //定义map集合并向其中添加值
            Map<String, Object> map = new HashMap<>();
            map.put(RETURN_CODE, Constants.SUCCESS_CODE);
            map.put(TYPE, AppType.NORMAL_TYPE);

            //定义输出流 并将错误结果返回给前端
            PrintWriter out = null;
            try {
                out = response.getWriter();
                String s = JSON.toJSONString(map);
                out.write(s);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            return;
        }


        //判断地址是否为空
        if (org.apache.commons.lang.StringUtils.isEmpty(authUrl) || org.apache.commons.lang.StringUtils.isEmpty(rDsgUrl)){
            ResponseUtils.sendResultMap(response, ResultCode.CAS_OR_RDSG_AUTH_URL_NULL);
            return;
        }

       /* CasServerRedirectEvent event = new CasServerRedirectEvent(request, response
                , applicationService, judgeLimit, rDsgUrl, portalToXDsgService, authUrl, responseRedirectService);
        event.setPortalService(portalService);
        event.setUserInfoService(userInfoService);
        event.setSubAccountRuleService(subAccountRuleService);
        //并且将发起cas或者lua的流程
        CasServerRedirec.redirecResult(event, redisClient);*/

        String uuid = (String) SessionUtils.getSessionByName(request, Constants.USERNAME);
        companyUuid = (String) SessionUtils.getSessionByName(request, Constants.COMPANY_UUID);
        String type=serviceUrl.substring(0, serviceUrl.indexOf("-"));
        serviceUrl=serviceUrl.substring(serviceUrl.indexOf("-")+1);
        request.getSession().removeAttribute(Constants.SERVICEURL);
        ssoLoginService.sendBaseInfo(serviceUrl.trim(),uuid,companyUuid,type.trim());
    }

    @Override
    public void setCookies(String key, String value) {
        redisClient.put(key, value);
    }


    public static void main(String[] args) {
        String serviceUrl="CAS-http://www.baidu.com?state=weeferfeg";
        String type=serviceUrl.substring(0, serviceUrl.indexOf("-"));
        System.out.println(type);
    }

}
