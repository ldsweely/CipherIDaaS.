package com.portal.controller;


import com.cipher.china.channel.AuthResult;
import com.cipher.china.channel.enums.AuthChannelEnum;
import com.cipher.china.channel.factory.AuthInfoFactory;
import com.cipher.china.channel.factory.AuthMachine;
import com.cipher.china.channel.pojo.AuthInfo;
import com.cipher.china.channel.pojo.AuthStrategy;
import com.portal.commons.*;
import com.portal.daoAuthoriza.RegisterApprovalMapper;
import com.portal.daoAuthoriza.UserGroupMapper;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.jms.JMSProducer;
import com.portal.jms.JMSType;
import com.portal.listener.CookieListener;
import com.portal.publistener.UserBehaviorPublistener;
import com.portal.redis.RedisClient;
import com.portal.service.*;
import com.portal.utils.CookieUtils;
import com.portal.utils.IpUtil;
import com.portal.utils.NumberUtil;
import com.portal.utils.SessionUtils;
import com.portal.utils.aes.AES;
import com.portal.utils.aes.AesKey;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.cipher.china.channel.enums.AuthChannelEnum.*;
import static com.cipher.china.channel.enums.AuthResultCode.*;
import static com.portal.commons.Constants.EXPIRES;
import static com.portal.commons.Constants.USERNAME;
import static com.portal.enums.ResultCode.*;
import static com.portal.enums.ResultCode.DING_PUSH_WAIT;
import static com.portal.enums.ResultCode.SUCCESS;

/**
 * 二次认证业务层
 * create by shizhao at 2019/5/18
 *
 * @author shizhao
 * @since 2019/5/18
 */
@RequestMapping(value = "/cipher/user")
@Controller
public class SecondAuthController extends BaseController implements CookieListener {

    private static final Logger logger = LoggerFactory.getLogger(SecondAuthController.class);

    @Autowired
    private UUIDService uuidService;

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
    private ISessionService sessionService;

    @Autowired
    private RegisterApprovalMapper registerApprovalMapper;

    @Autowired
    private UserBehaviorPublistener userBehaviorPublistener;

    @Autowired
    private RedisClient<String, Object> redisClient = new RedisClient<>();

    @Autowired
    private OtpDynamicInfoService otpDynamicInfoService;

    @Autowired
    private CheckUserService checkUserService;

    @Autowired
    private UserGroupMapper userGroupMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JMSProducer jmsProducer;

    @RequestMapping(value = "/regist")
    @ResponseBody
    public Map<String, Object> regist(HttpServletRequest request,
                                      @RequestParam(value = "companyUUid") String companyUUid,
                                      @RequestParam(value = "userName") String accountNumber,
                                      @RequestParam(value = "phoneNumber") String phoneNumber,
                                      @RequestParam(value = "mail", required = false) String mail,
                                      @RequestParam(value = "idNum", required = false) String idNum,
                                      @RequestParam(value = "platId", required = false) String platId,
                                      @RequestParam(value = "dingUserId", required = false) String dingUserId,
                                      @RequestParam(value = "dingUnionId", required = false) String unionId,
                                      @RequestParam(value = "wxUserId", required = false) String wxUserId) {

        //入参校验
        if (isEmpty(companyUUid, phoneNumber)) {
            logger.warn("There Is Empty Info As The companyUUid =[{}] mail=[{}] phoneNumber=[{}]  ",
                    companyUUid, phoneNumber);
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        UserInfoDomain userInfo = userInfoService.getCompanyUserInfoByPhoneNumber(phoneNumber, companyUUid);

        if (userInfo == null && StringUtils.isNotEmpty(mail)) {
            userInfo = userInfoService.getCompanyUserInfoByMail(mail, companyUUid);
        }

        if (userInfo != null) {
            logger.info("User Already Exist ..");

            return sendBaseErrorMap(REGIST_EXIST_LOCAL);
        }

        //获取注册流程
        AccountRegistration accountRegistration = identityModesService.getAccountRegistration(companyUUid);

        boolean isPhoneChecked = redisClient.containsKey(CacheKey.getCacheKeyUserRegisterPhone(phoneNumber));

        boolean isMailChecked = redisClient.containsKey(CacheKey.getCacheKeyUserRegisterMail(mail));

        //邮箱验证开启且不可跳过
        if (accountRegistration.getIsOpenMailReg() == Constants.AUTH_ON && accountRegistration.getIsSkipMailReg() != Constants.AUTH_ON) {
            //如果邮箱未验证 返回认证失败
            if (!isMailChecked) {
                logger.info("Mail =[{}] Need Checked", mail);
                return sendBaseErrorMap(LOGIN_AUTH_FAILED);
            }
        }

        //手机验证开启 且不可跳过
        if (accountRegistration.getIsOpenNumReg() == Constants.AUTH_ON && accountRegistration.getIsSkipNumReg() != Constants.AUTH_ON) {
            //如果手机未验证 返回认证失败
            if (!isPhoneChecked) {
                logger.info("Phone =[{}] Need Checked", phoneNumber);
                return sendBaseErrorMap(LOGIN_AUTH_FAILED);
            }
        }
//
//        //如果手机和邮箱都没验证 返回认证失败  手机和邮箱必须验证一个
//        if (!isMailChecked && !isPhoneChecked) {
//            return sendBaseErrorMap(ResultCode.PHONE_MAIL_ERROR);
//        }

        //创建注册人的对象
        RegisterApprovalDomain registerApprovalDomain =
                objectFactoryService.userCreate(companyUUid, accountNumber, phoneNumber, mail);
        registerApprovalDomain.setDingUserId(dingUserId);
        registerApprovalDomain.setIdNum(idNum);
        registerApprovalDomain.setPlatId(platId);
        registerApprovalDomain.setUnionId(unionId);
        registerApprovalDomain.setWxUserId(wxUserId);

        //查看注册表中 是否已经存在该用户
//        RegisterApprovalDomain registerApprovalDomain1 = userInfoService.getRegistUser(registerApprovalDomain);
//        if (registerApprovalDomain1 != null) {
//            logger.info("Register User Already Exist..");
//            return sendBaseErrorMap(ResultCode.REGIST_EXIST);
//        }

        //注册到数据库
//        if (!userInfoService.registUser(registerApprovalDomain)) {
//            logger.info("Register Failed As An Exception Caught..");
//            return sendBaseErrorMap(USER_REGISTER_MEET_AN_EXCEPTION);
//        }
        Integer flag = checkUserService.checkUserInfo(companyUUid, "", registerApprovalDomain.getUserEmail(), registerApprovalDomain.getPhoneNumber());
        if (flag.equals(1)) {
            return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERACCOUNTNUMBER);
        } else if (flag.equals(2)) {
            return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERMAIL);
        } else if (flag.equals(3)) {
            return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERTELEPHONE);
        } else {

            Integer service = registerApprovalMapper.serviceStateBycompanyUuid(companyUUid);
                    /*//如果是邮件通知 ，则必须使邮件通知服务开启
                    if(StringUtils.isNotEmpty(registerApprovalDomain.getUserEmail())&&service.equals(1)){
                        return NewReturnUtils.failureResponse(ReturnMsg.APPROVALEMAIL);
                    }*/
            if (registerApprovalDomain != null ) {
                //审批时：赛赋扫码，钉钉扫码，大白扫码 如果发生在该公司下重复，则注册失败
                boolean platFlag = false;
                boolean dabbyFlag = false;
                boolean dingFlag = false;
                //赛赋扫码是否发生重复
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getPlatId())) {
                    int a = registerApprovalMapper.selectPlatByPlatId(registerApprovalDomain.getPlatId(), companyUUid);
                    if (a > 0) {
                        platFlag = true;
                    }
                }
                //大白扫码是否发生重复
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getIdNum())) {
                    int b = registerApprovalMapper.selectDabbyByIdNum(registerApprovalDomain.getIdNum(), companyUUid);
                    if (b > 0) {
                        dabbyFlag = true;
                    }
                }
                //钉钉扫码是否发生重复
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getDingUserId()) && org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getUnionId())) {
                    int c = registerApprovalMapper.selectDingByDingId(registerApprovalDomain.getDingUserId(), registerApprovalDomain.getUnionId(), companyUUid);
                    if (c > 0) {
                        dingFlag = true;
                    }
                }
                if (platFlag || dabbyFlag || dingFlag) {
                    if (platFlag) {
                        return NewReturnUtils.failureResponse(ReturnMsg.PLATEWMAUTH);
                    }
                    if (dabbyFlag) {
                        return NewReturnUtils.failureResponse(ReturnMsg.DABBYEWMAUTH);
                    }
                    if (dingFlag) {
                        return NewReturnUtils.failureResponse(ReturnMsg.DINGEWMAUTH);
                    }
                }
                //没有发生重复继续执行
                String userId = uuidService.getUUid();
                RegisterUserInfo registerUserInfo = new RegisterUserInfo();
                registerUserInfo.setUuid(userId);
                registerUserInfo.setCompanyId(companyUUid);
                registerUserInfo.setAccountNumber(registerApprovalDomain.getPhoneNumber());
                registerUserInfo.setUserName(registerApprovalDomain.getUserName());
                registerUserInfo.setMail(registerApprovalDomain.getUserEmail());
                registerUserInfo.setPhoneNumber(registerApprovalDomain.getPhoneNumber());
                //向用户表添加一条用户信息
                registerApprovalMapper.insertRegisterUser(registerUserInfo);

                //审批通过后，分别向对应表中添加数据（赛赋扫码，大白扫码，钉钉扫码）
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getPlatId())) {
                    PlatEwmDomain platEwmDomain = new PlatEwmDomain();
                    platEwmDomain.setPlatId(registerApprovalDomain.getPlatId());
                    platEwmDomain.setUserId(userId);
                    platEwmDomain.setCompanyId(companyUUid);
                    registerApprovalMapper.insertPlat(platEwmDomain);
                }
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getIdNum())) {
                    DabbyEwmDomain dabbyEwmDomain = new DabbyEwmDomain();
                    dabbyEwmDomain.setUserId(userId);
                    dabbyEwmDomain.setCompanyId(companyUUid);
                    dabbyEwmDomain.setLinkAccount(registerApprovalDomain.getIdNum());
                    registerApprovalMapper.insertDabby(dabbyEwmDomain);
                }
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getDingUserId()) && org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getUnionId())) {
                    DingEwmDomain dingEwmDomain = new DingEwmDomain();
                    dingEwmDomain.setUserId(userId);
                    dingEwmDomain.setCompanyId(companyUUid);
                    dingEwmDomain.setDingUserId(registerApprovalDomain.getDingUserId());
                    dingEwmDomain.setDingUnionId(registerApprovalDomain.getUnionId());
                    registerApprovalMapper.insertDing(dingEwmDomain);
                }

                //向密码表添加一条密码
                String password = NumberUtil.getStringRandom(8);
                String encryptPassword = AES.encryptToBase64(password, AesKey.AES_KEY);
                registerApprovalMapper.insertRegisterPwd(userId, registerApprovalDomain.getPhoneNumber(), encryptPassword);

                //添加到根节点
                GroupUserMapDomain groupUserMapDomain = new GroupUserMapDomain();
                groupUserMapDomain.setUserId(userId);
                groupUserMapDomain.setGroupId(1);
                userGroupMapper.insertUserGroupMap(groupUserMapDomain);
                //发送账号密码，优先邮件（邮件或短信）
                if (org.apache.commons.lang.StringUtils.isNotEmpty(registerApprovalDomain.getUserEmail()) && service.equals(0)) {
                    //发送邮件
                    EmailAccountInfoDomain emailAccountInfoDomain = new EmailAccountInfoDomain();
                    emailAccountInfoDomain.setCompanyId(companyUUid);
                    emailAccountInfoDomain.setMail(registerApprovalDomain.getUserEmail());
                    emailAccountInfoDomain.setAccount(registerApprovalDomain.getPhoneNumber());
                    emailAccountInfoDomain.setPassword(password);
                    jmsProducer.sendMessage(emailAccountInfoDomain, JMSType.EMAIL_SEND_INFORM);
                } else {
                    //发送短信
                    SmsAccountInfoDomain smsAccountInfoDomain = new SmsAccountInfoDomain();
                    smsAccountInfoDomain.setAccount(registerApprovalDomain.getPhoneNumber());
                    smsAccountInfoDomain.setPassword(password);
                    smsAccountInfoDomain.setTelephone(registerApprovalDomain.getPhoneNumber());
                    smsAccountInfoDomain.setCompanyId(companyUUid);
                    jmsProducer.sendMessage(smsAccountInfoDomain, JMSType.SMS_SEND_INFORM);
                }
            }
        }

        return sendBaseNormalMap(SUCCESS);

}

    /**
     * 获取二次认证流程
     *
     * @param companyUUid 公司ID
     * @param userId      用户的唯一标识
     */
    @RequestMapping(value = "/secondLoginFlow")
    @ResponseBody
    public Map<String, Object> onceLoginFlow(String companyUUid, String userId) {

        //入参校验
        if (StringUtils.isBlank(companyUUid) || StringUtils.isBlank(userId)) {

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        Map<String, Object> map = new HashMap<>();


        //获取用户的身份认证策略
        IdentityAuthTypeDomain identityAuthTypeDomain = resetPasswordService.getIdentityAuthType(companyUUid, userId);

        //获取二次认证的方式
        String authModes = identityAuthTypeDomain.getSecondAuthType();

        //创建二次认证对象
        SecondLoginInfo secondLoginInfo = null;

        //如果二次认证方式不为空设置二次认证的方式
        if (!StringUtils.isBlank(authModes)) {

            JSONObject jsonObject = JSONObject.fromObject(authModes);
            secondLoginInfo = (SecondLoginInfo) JSONObject.toBean(jsonObject, SecondLoginInfo.class);
            map.put(Constants.SECOND_LOGIN_INFO, secondLoginInfo);
        }
        map.put(Constants.USER_ID, userId);

        return sendBaseNormalMap(map);
    }

    /**
     * 赛赋认证二次认证扫码
     *
     * @param uuid        赛赋认证的唯一ID
     * @param companyUUid 公司的ID
     * @return
     */
    @RequestMapping(value = "/saifuSecondPolling")
    @ResponseBody
    public Map<String, Object> getSaifuPolling(HttpServletRequest request, HttpServletResponse response,
                                               String uuid, String companyUUid, String userId) {

        //入参校验
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(companyUUid)) {
            logger.warn("enter OnceLoginController failed but param uuid=[{}] companyUuid = [{}]" + new Object[]{uuid, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setSaifuScan(Constants.ON);

        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createCipherScanAuth(uuid);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        AuthResult authResult = authMachine.getTheResult();
        //判断扫码认证是否成功
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取赛赋认证是否绑定本地账号
        UserInfoDomain userInfoDomainBySaifu = userInfoService.getUserBySaifu(authResult.getUserName(), companyUUid);

        //判断用户是否绑定赛赋账号 没有绑定直接返回认证失败
        if (userInfoDomainBySaifu == null) {
            return sendBaseErrorMap(NOT_BINGDING);
        }

        if (!userId.equals(userInfoDomainBySaifu.getUuid())) {
            logger.info("Scan User Not Matched..");
            return sendBaseErrorMap(LOGIN_SECOND_NOT_MATCHED);
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(userInfoDomainBySaifu.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }


        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);
        //设置赛赋验证已通过
        authFlow.setSaiFuBindLocalUuid(userInfoDomainBySaifu.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(userInfoDomainBySaifu.getUuid()), authFlow, EXPIRES);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session ,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, uuid);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "获取二次认证流程", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfoDomainBySaifu.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, userInfoDomainBySaifu.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);

        }

        return sendBaseNormalMap(SUCCESS);
    }

    /**
     * 赛赋totp认证二次认证扫码
     *
     * @param userId      用户的唯一标识
     * @param companyUUid 公司的ID
     * @param totpCode    动态验证码
     * @return
     */
    @RequestMapping(value = "/totpSecondCheck")
    @ResponseBody
    public Map<String, Object> totpSecondCheck(HttpServletRequest request, HttpServletResponse response, String userId, String companyUUid, String totpCode) {
        //入参校验
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(companyUUid) || StringUtils.isEmpty(totpCode)) {
            logger.warn("enter OnceLoginController failed but param uuid=[{}] companyUuid = [{}]",
                    new Object[]{companyUUid, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfoByUUid(userId);

        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setTotpAuth(Constants.ON);
        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createTotpAuth(user.getUuid(), totpCode);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();
        //判断扫码认证是否成功
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);
        //设置赛赋验证已通过
        authFlow.setSaiFuTotpLocalUuid(user.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "赛赋totp认证二次认证成功", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, user.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);

        }

        return sendBaseNormalMap(SUCCESS);
    }

    /**
     * 获取dingding扫码状态
     *
     * @param code
     * @param companyUUid
     * @return
     */
    @RequestMapping(value = "/dingTalkSecondAuth")
    @ResponseBody
    public Map<String, Object> getDingdingPolling(HttpServletRequest request, HttpServletResponse response, String code, String companyUUid, String userId) {

        //入参校验
        if (isEmpty(code, companyUUid)) {
            logger.warn("Check failed As code=[{}] companyUuid = [{}]" + new Object[]{code, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);

        }
        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setDingdingScan(Constants.ON);

        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createDingTalkScanAuth(code);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //判断扫码认证是否成功
        AuthResult authResult = authMachine.getTheResult();
        if (!authMachine.isSuccess()) {
            authResult = authMachine.getTheFailedResult();
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取与钉钉绑定的账号
        UserInfoDomain userInfo =
                userInfoService.getUserByDingTalkByUnionIdAndCompanyId(authResult.getUserName(), companyUUid);

        //判断用户是否绑定钉钉 没有绑定就直接返回
        if (userInfo == null) {
            return sendBaseErrorMap(NOT_BINGDING);
        }

        if (!userId.equals(userInfo.getUuid())) {
            return sendBaseErrorMap(LOGIN_SECOND_NOT_MATCHED);
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(userInfo.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }


        //设置钉钉验证已通过
        authFlow.setDingBindLocalUuid(userInfo.getUuid());

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }

        //把扫码成功的结果放入缓存中
        redisClient.put(CacheKey.getCacheKeyUserAuth(userInfo.getUuid()), authFlow, EXPIRES);
        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "获取钉钉扫码状态", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfo.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, userInfo.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }


        return sendBaseNormalMap();
    }

    /**
     * 大白二次认证
     *
     * @param uuid        大白的表示
     * @param companyUUid 公司的唯一ID
     */
    @RequestMapping(value = "/daBaiSecondPolling")
    @ResponseBody
    public Map<String, Object> getDabaiPolling(HttpServletRequest request, HttpServletResponse response, String uuid, String companyUUid, String userId) {

        //入参校验
        if (StringUtils.isBlank(uuid) || StringUtils.isBlank(companyUUid)) {
            logger.warn("enter OnceLoginController.getDabaiPolling failed but param uuid=[{}] companyUuid = [{}]" + new Object[]{uuid, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);

        }
        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setDabaiScan(Constants.ON);

        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createBayMaxScanAuth(uuid);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();
        //判断扫码认证是否成功
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //获取大白绑定的本地账号
        UserInfoDomain userInfoDomainByDabai = userInfoService.getUserByDabai(authResult.getUserName(), companyUUid);

        //判断大白认证是否绑定本地账号 没有绑定直接返回认证失败
        if (userInfoDomainByDabai == null) {
            return sendBaseErrorMap(NOT_BINGDING);
        }

        if (!userId.equals(userInfoDomainByDabai.getUuid())) {
            return sendBaseErrorMap(LOGIN_SECOND_NOT_MATCHED);
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(userInfoDomainByDabai.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }


        //设置大白验证已通过
        authFlow.setDaBaiBindLocalUuid(userInfoDomainByDabai.getUuid());
        //把扫码成功的结果放入缓存中
        redisClient.put(CacheKey.getCacheKeyUserAuth(userInfoDomainByDabai.getUuid()), authFlow, EXPIRES);
        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "大白二次认证", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfoDomainByDabai.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, userInfoDomainByDabai.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }

        return sendBaseNormalMap();
    }

    /**
     * 邮箱验证
     *
     * @param companyUUid 公司的ID
     * @param mail        待校验的邮箱
     * @param code        邮箱验证码
     * @return
     */
    @RequestMapping(value = "/mailCodeSecondChecked")
    @ResponseBody
    public Map<String, Object> emailChecked(HttpServletRequest request, HttpServletResponse response, String companyUUid, String mail, String code, String userId) {

        if (StringUtils.isBlank(companyUUid) || StringUtils.isBlank(mail) || StringUtils.isBlank(code)) {
            logger.warn(" There Is Empty Info As companyUUid =[{}] accountNumber=[{}] mail=[{}] code=[{}] ",
                    new Object[]{companyUUid, mail, code});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //打开mail验证的开关
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setMailRandomCode(Constants.ON);

        //设置要验证的信息
        AuthInfo authInfo = AuthInfoFactory.createMailCodeAuth(mail, code);
        //获取验证结果
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        AuthResult authResult = authMachine.getTheResult();
        if (!authMachine.isSuccess()) {
            logger.info("Mail Code Auth Failed");

            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //认证成功获取认证的主账号
        UserInfoDomain userInfoDomainByMail = userInfoService.getUserInfoByMail(mail);

        //判断根据邮箱搜索是否能查出主账号 如果为空直接返回认证失败
        if (userInfoDomainByMail == null) {
            return sendBaseErrorMap(ResultCode.BIND_FLOW_MAIL_NOT_REGISTER);
        }

        if (!userId.equals(userInfoDomainByMail.getUuid())) {
            return sendBaseErrorMap(LOGIN_SECOND_NOT_MATCHED);
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(userInfoDomainByMail.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);
        //设置邮箱验证已通过
        authFlow.setMail(AuthFlow.SUCCESS);
        redisClient.put(CacheKey.getCacheKeyUserAuth(userInfoDomainByMail.getUuid()), authFlow, EXPIRES);
        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "邮箱验证", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfoDomainByMail.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, userInfoDomainByMail.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }
        return sendBaseNormalMap(SUCCESS);

    }

    /**
     * 手机验证码发送
     *
     * @param companyUUid 公司ID
     * @param phoneNumber 待校验的手机号
     * @param code        手机验证码
     * @return
     */
    @RequestMapping(value = "/phoneCodeSecondChecked")
    @ResponseBody
    public Map<String, Object> getPhoneCheck(HttpServletRequest request, HttpServletResponse response,
                                             String companyUUid, String phoneNumber, String code, String userId) {

        //入参校验
        if (StringUtils.isBlank(companyUUid) || StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(code)) {
            logger.warn("There Is Empty Info As companyUUid =[{}] phoneNumber=[{}] code=[{}] code=[{}] ",
                    new Object[]{companyUUid, phoneNumber, code});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //创建手机认证方式并且去认证中心认证
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setPhoneRandomCode(Constants.ON);
        AuthInfo authInfo = AuthInfoFactory.createSmsCodeAuth(phoneNumber, code);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();
        if (!authResult.isSuccess()) {
            logger.info("Phone Code Check Failed As The Code =[{}]", code);

            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //认证成功 获取认证的主账号
        UserInfoDomain userInfoDomainByPhone = userInfoService.getUserInfoByPhone(phoneNumber);

        //判断userDomain是否为空
        if (userInfoDomainByPhone == null) {
            return sendBaseErrorMap(BIND_ERROR_PHONE_NULL);
        }

        if (!userId.equals(userInfoDomainByPhone.getUuid())) {
            return sendBaseErrorMap(LOGIN_SECOND_NOT_MATCHED);
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(userInfoDomainByPhone.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);
        //认证成功 把认证成功结果放入缓存中
        authFlow.setPhoneNumber(AuthFlow.SUCCESS);
        redisClient.put(CacheKey.getCacheKeyUserAuth(userInfoDomainByPhone.getUuid()), authFlow, EXPIRES);


        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "手机随机码发送", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfoDomainByPhone.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, userInfoDomainByPhone.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);

        }

        return sendBaseNormalMap(SUCCESS);
    }


    /**
     * weixin扫码
     *
     * @param code
     * @param companyUUid
     * @return
     */
    @RequestMapping(value = "/weixinSecondAuth")
    @ResponseBody
    public Map<String, Object> weixinSecondAuth(HttpServletRequest request, HttpServletResponse response, String code, String companyUUid, String userId) {

        //入参校验
        if (isEmpty(code, companyUUid)) {
            logger.warn("Check failed As code=[{}] companyUuid = [{}]" + new Object[]{code, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);

        }
        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setWeixinScan(Constants.ON);

        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createWeixinAuth(code);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        //判断扫码认证是否成功
        AuthResult authResult = authMachine.getTheResult();
        if (!authMachine.isSuccess()) {
            authResult = authMachine.getTheFailedResult();
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }


        logger.info("weixin san info auth result:  " + authResult.getUserName());

        // String uu="Xia";
        //获取与微信绑定的账号
        UserInfoDomain userInfo =
                userInfoService.selectUserInfoByWeixinInfo(authResult.getUserName(), companyUUid);

        //判断用户是否绑定微信 没有绑定就直接返回
        if (userInfo == null) {
            logger.info("weixin sencond scan bind info is null");
            return sendBaseErrorMap(NOT_BINGDING);
        }

        if (!userId.equals(userInfo.getUuid())) {
            return sendBaseErrorMap(LOGIN_SECOND_NOT_MATCHED);
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(userInfo.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }


        //设置微信验证已通过
        authFlow.setWeixinLocalUuid(userInfo.getUuid());

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }

        //把扫码成功的结果放入缓存中
        redisClient.put(CacheKey.getCacheKeyUserAuth(userInfo.getUuid()), authFlow, EXPIRES);
        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.ACCOUNT_MAINTAIN, IpUtil.getIp(), "获取钉钉扫码状态", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            //   redisClient.remove(CacheKey.getCacheKeyUserAuth(userInfo.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, userInfo.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }


        return sendBaseNormalMap();
    }


/**
 * dotp认证二次认证扫码
 *
 * @param userId       用户的唯一标识
 * @param companyUUid  公司的ID
 * @param dingTotpCode 动态验证码
 * @return
 *//*

    @RequestMapping(value = "/dingTotpSecondCheck")
    @ResponseBody
    public Map<String, Object> dingTotpSecondCheck(HttpServletRequest request, HttpServletResponse response, String userId, String companyUUid, String dingTotpCode) {
        //入参校验
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(companyUUid) || StringUtils.isEmpty(dingTotpCode)) {
            logger.warn("enter dingTotpSecondCheck failed but param uuid=[{}] companyUuid = [{}]",
                    new Object[]{userId, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfoByUUid(userId);

        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setDingTotpCode(Constants.ON);
        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createDingTotpAuth(user.getUuid(), dingTotpCode);
        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();
        //判断扫码认证是否成功
        if (!authMachine.isSuccess()) {
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));
        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);
        //设置钉钉动态码验证已通过
        authFlow.setDingBindLocalUuid(user.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);


        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }


        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "获取totp二次认证扫码", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));

            //设置session
            SessionUtils.setSession(request,response, USERNAME, user.getUuid());
            SessionUtils.setSession(request,response, Constants.COMPANY_UUID, companyUUid);
        }
        return sendBaseNormalMap(SUCCESS);
    }
*/


    /**
     * 钉钉push认证二次认证扫码
     *
     * @param userId      用户的唯一标识
     * @param companyUUid 公司的ID
     * @param
     * @return
     */
    @RequestMapping(value = "/dingPushSecondCheck")
    @ResponseBody
    public Map<String, Object> dingPushSecondCheck(HttpServletRequest request, HttpServletResponse response, String userId, String companyUUid) {
        //入参校验
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(companyUUid)) {
            logger.warn("enter dingTotpSecondCheck failed but param uuid=[{}] companyUuid = [{}]",
                    new Object[]{userId, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        String cacheKey = CacheKey.getDingPushReturnResultKey(userId);
        redisTemplate.delete(cacheKey);

        logger.info("enter secondAuthController cacheKey " + redisClient.get(cacheKey));

        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfoByUUid(userId);

        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setDingPushAuth(Constants.ON);
        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createDingPushAuth(user.getUuid(), IpUtil.getIp());


        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);

        AuthResult authResult = authMachine.getTheResult();


        //判断钉钉push认证是否成功
        if (!authMachine.isSuccess()) {
            logger.info("enter secondAuthController dingPushSecondCheck is failed" + authResult.getResultMessage());
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        logger.info("enter secondAuthController dingPushSecondCheck is success" + authResult.getResultMessage());

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));

        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);

        logger.info("enter secondAuthController dingPushSecondCheck secondLoginInfo" + secondLoginInfo);

        //设置钉钉push验证已通过
        authFlow.setDingPushLocalUuid(user.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }


        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "钉钉push二次认证", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, user.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }

        logger.info("enter secondAuthController dingPushSecondCheck response" + sendBaseNormalMap(SUCCESS));

        return sendBaseNormalMap(SUCCESS);
    }


    @RequestMapping(value = "/dingPushPolling")
    @ResponseBody
    public Map<String, Object> dingPushPolling(HttpServletRequest request, HttpServletResponse response, String userId, String companyUUid) {

        //入参校验
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(companyUUid)) {
            logger.warn("enter dingPushPolling failed but param uuid=[{}] companyUuid = [{}]",
                    new Object[]{userId, companyUUid});
            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        String cacheKey = CacheKey.getDingPushReturnResultKey(userId);

        logger.warn("++++++++++++++++cacheKey++++++++++++++++" + redisClient.get(cacheKey));

        //不包含这个key,waiting状态
        if (!redisClient.containsKey(cacheKey)) {
            return sendBaseErrorMap(DING_PUSH_WAIT);
        } else {
            Integer code = (Integer) redisClient.get(cacheKey);
            redisTemplate.delete(cacheKey);
            //超时
            if (code == DING_PUSH_TIME_OUT.getResultCode()) {
                return sendBaseErrorMap(DING_PUSH_TIMEOUT);
            }
            //失败
            if (code == DING_PUSH_CHECK_FAILED.getResultCode()) {
                return sendBaseErrorMap(DING_PUSH_FAILE);
            }

        }

        redisTemplate.delete(cacheKey);
        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfoByUUid(userId);

        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));

        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);

        logger.info("enter secondAuthController dingPushSecondCheck secondLoginInfo" + secondLoginInfo);

        //设置钉钉push验证已通过
        authFlow.setDingPushLocalUuid(user.getUuid());
        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }


        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "钉钉push二次认证", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, user.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }

        logger.info("enter secondAuthController dingPushSecondCheck response" + sendBaseNormalMap(SUCCESS));

        return sendBaseNormalMap(SUCCESS);

    }

    /**
     * OTP 二次认证
     *
     * @param userId
     * @param companyUUid
     */
    @RequestMapping(value = "/otpDynamicSecondCheck")
    @ResponseBody
    public Map<String, Object> otpDynamicSecondCheck(HttpServletRequest request, HttpServletResponse response, String userId, String companyUUid, String otpDynamicCode) {
        //入参校验
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(companyUUid)) {
            logger.warn("enter otpDynamicSecondCheck failed but param uuid=[{}] companyUuid = [{}]",
                    new Object[]{userId, companyUUid});

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取用户信息
        UserInfoDomain user = userInfoService.getUserInfoByUUid(userId);

        //创建认证方式
        AuthStrategy authStrategy = new AuthStrategy();
        authStrategy.setOtpDynamicCode(Constants.ON);
        //设置认证实体
        AuthInfo authInfo = AuthInfoFactory.createOtpDynamicAuth(userId, otpDynamicCode);

        AuthMachine authMachine = portalAuthChannelService.auth(authStrategy, authInfo, companyUUid);
        AuthResult authResult = authMachine.getTheResult();

        //判断otp动态码认证是否成功
        if (!authMachine.isSuccess()) {
            logger.info("enter secondAuthController otpDynamicSecondCheck is failed" + authResult.getResultMessage());
            return sendTheMap(authResult.getResultCode(), authResult.getResultMessage());
        }

        //otp下发的秘钥验证成功后保存
        String dynamicKey = (String) redisClient.get(CacheKey.getCacheKeyUserOtpSecrect(userId));
        UserInfoDomain userInfoDomain = new UserInfoDomain();
        userInfoDomain.setUuid(userId);
        if (StringUtils.isNotEmpty(dynamicKey)) {
            String otpkey = otpDynamicInfoService.getOtpDynamicInfo(userId);
            if (StringUtils.isNotEmpty(otpkey)) {
                otpDynamicInfoService.deleteOtpDynamicInfo(userId);
            }
            String uuid = UUID.randomUUID().toString().replace("-", "").toLowerCase();
            OtpDynamicInfo otpDynamicInfo = new OtpDynamicInfo(userId, dynamicKey, uuid, companyUUid);
            otpDynamicInfoService.insertOtpDynamicInfo(otpDynamicInfo);
            //设置totp秘钥下发状态
            userInfoDomain.setIssueStatus(1);
            userInfoService.updateUserInfo(userInfoDomain);

            redisClient.remove(CacheKey.getCacheKeyUserOtpSecrect(userId));
        }


        //判断redis是否有该对象
        AuthFlow authFlow = (AuthFlow) redisClient.get(CacheKey.getCacheKeyUserAuth(user.getUuid()));

        if (authFlow == null) {
            authFlow = new AuthFlow();
        }

        SecondLoginInfo secondLoginInfo = resetPasswordService.getSecondLoginInfo(companyUUid, userId);

        logger.info("enter secondAuthController otpDynamicSecondCheck secondLoginInfo" + secondLoginInfo);

        //设置钉钉push验证已通过
        authFlow.setSaiFuTotpLocalUuid(user.getUuid());

        redisClient.put(CacheKey.getCacheKeyUserAuth(user.getUuid()), authFlow, EXPIRES);

        //判断二次认证是串行还是并行 true：并行 false:串行
        if (secondLoginInfo.getSwitches() == 0) {
            secondLoginInfo = null;
        }

        if (identityModesService.isAuthComplete(authFlow, secondLoginInfo)) {
            //TODO:增加session,cookie
            sessionService.updateSessionExpireTime(IpUtil.getIp(), companyUUid, userId);
            try {
                UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo(userId, null, ConstantsCMP.UserBehaviorConstant.APPLICATION_VISIT, IpUtil.getIp(), "otp动态码认证", companyUUid);
                userBehaviorPublistener.publish(userBehaviorInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            CookieUtils.writePortalCookie(response, userId, this::setCookies);
            redisClient.remove(CacheKey.getCacheKeyUserAuth(user.getUuid()));

            //设置session
            SessionUtils.setSession(request, response, USERNAME, user.getUuid());
            SessionUtils.setSession(request, response, Constants.COMPANY_UUID, companyUUid);
        }

        return sendBaseNormalMap(SUCCESS);
    }


    @Override
    public void setCookies(String key, String value) {
        redisClient.put(key, value);
    }


}
