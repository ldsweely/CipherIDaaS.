package com.portal.service.impl;
import com.google.gson.Gson;
import com.portal.commons.CacheKey;
import com.portal.commons.ConstantType;
import com.portal.daoAuthoriza.IdentityModesDAO;
import com.portal.domain.*;
import com.portal.redis.RedisClient;

import com.portal.service.IdentityModesService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * @Author: TK
 * @Date: 2019/4/28 10:11
 */

@Service
public class IdentityModesServiceImpl implements IdentityModesService {

    @Autowired
    private IdentityModesDAO identityModesDAO;

    @Autowired
    private RedisClient<String, Object> redisClient = new RedisClient<>();

    @Override
    public IdentitySettingInfoDomain getIdentityModesByCompanyUUid(String companyUuid) {
        //入参校验
        if (StringUtils.isEmpty(companyUuid)){
            return null;
        }
        return identityModesDAO.getIdentityModesByCompanyUUid(companyUuid);
    }

    @Override
    public PortalConfigDomain getPortalConfig(String companyUuid) {

        //入参校验
        if (companyUuid.isEmpty()){
            return null;
        }

        return identityModesDAO.getPortalConfig(companyUuid) ;
    }

    @Override
    public IdentityAuthentication getIdentityAuthenticationByCompanyId(String companyId) {
        IdentitySettingInfoDomain identitySettingInfoDomain = getIdentityModesByCompanyUUid(companyId);
        //没有找到对应的配置信息
        if (identitySettingInfoDomain == null){
            return null;
        }

        //配置信息没有具体内容
        if (StringUtils.isEmpty(identitySettingInfoDomain.getAuthMode())){
            return null;
        }

        String authMode = identitySettingInfoDomain.getAuthMode();

        try {
            //进行格式化
            IdentityAuthentication identityAuthentication = new Gson().fromJson(authMode,IdentityAuthentication.class);

            return identityAuthentication;
        }catch (Exception e){
            //格式化失败
            return null;
        }
    }

    @Override
    public AccountBinding getAccountBindingByCompanyId(String companyId) {
        IdentitySettingInfoDomain identitySettingInfoDomain = getIdentityModesByCompanyUUid(companyId);
        //没有找到对应的配置信息
        if (identitySettingInfoDomain == null){
            return null;
        }

        //配置信息没有具体内容
        if (StringUtils.isEmpty(identitySettingInfoDomain.getBindingFlow())){
            return null;
        }
        String accountBinding = identitySettingInfoDomain.getBindingFlow();
        try {
            //进行格式化
            return new Gson().fromJson(accountBinding, AccountBinding.class);
        }catch (Exception e){
            //格式化失败
            return null;
        }
    }

    @Override
    public ForgetPassword getForgetPasswordFlow(String companyId) {
        //获取身份认证设置
        IdentitySettingInfoDomain identitySettingInfoDomain = getIdentityModesByCompanyUUid(companyId);

        //新建要验证的实体类
        UserInfoDomain userInfoDomain=null;

        //将字符串转换为实体类获取忘记密码流程
        ForgetPassword forgetPassword = null;
        try {
            //进行格式化
            return new Gson().fromJson(identitySettingInfoDomain.getForgetFlow(),ForgetPassword.class);
        }catch (Exception e){
            return null;
        }

    }

    @Override
    public AccountRegistration getAccountRegistration(String companyId) {
        IdentitySettingInfoDomain identitySettingInfoDomain = getIdentityModesByCompanyUUid(companyId);


        if (identitySettingInfoDomain == null){
            return null;
        }

        if (StringUtils.isEmpty(identitySettingInfoDomain.getRegistFlow())){
            return null;
        }

        try {
            return new Gson().fromJson(identitySettingInfoDomain.getRegistFlow(),AccountRegistration.class);
        }catch (Exception e){
            return null;
        }

    }

    @Override
    public ForgetPassword getForgetPassword(String companyId) {
        //获取身份认证设置
        IdentitySettingInfoDomain identitySettingInfoDomain = getIdentityModesByCompanyUUid(companyId);

        //将字符串转换为实体类获取忘记密码流程
        ForgetPassword forgetPassword = null;
        try {
            return new Gson().fromJson(identitySettingInfoDomain.getForgetFlow(),ForgetPassword.class);
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public boolean isBindingAuthComplete(UserInfoDomain userInfo, AccountBinding accountBinding, int on) {


        if (userInfo == null){
            return false;
        }

        if (StringUtils.isEmpty(userInfo.getUuid())){
            return false;
        }

        String userId = userInfo.getUuid();

        //是否进行手机校验
        boolean isNeedPhoneCheck = accountBinding.getIsOpenNumBind() == on?true:false;

        //是否进行邮箱校验
        boolean isNeedMailCheck = accountBinding.getIsOpenMailBind() == on?true:false;

        //判断用户是否进行手机校验
        boolean isPhoneChecked =true;

        //判断用户是否进行邮箱校验
        boolean isMailChecked = true;

        //用户校验的邮箱
        String userCheckMail = null;

        //用户校验的手机号
        String userCheckPhone = null;

        //用户的邮箱
        String userMail = userInfo.getMail();

        //用户的手机
        String userPhone =userInfo.getPhoneNumber();

        //需要进行手机校验则获取信息
        if (isNeedPhoneCheck){
            //获取校验用户的手机号
            userCheckPhone = (String)redisClient.get(CacheKey.getCacheKeyUserBindingPhone(userId));
            isPhoneChecked = StringUtils.isNoneBlank(userCheckPhone)?true:false;
        }

        //需要进行邮箱校验则获取信息
        if (isNeedMailCheck){
            //获取校验用户的邮箱
            userCheckMail = (String)redisClient.get(CacheKey.getCacheKeyUserBindingMail(userId));
            isMailChecked = StringUtils.isNoneBlank(userCheckMail)?true:false;
        }


        //需要进行手机校验，但手机未校验
        if (isNeedPhoneCheck && !isPhoneChecked){
            return false;
        }

        //需要进行邮箱校验，但邮箱未校验
        if (isNeedMailCheck && !isMailChecked){
            return false;
        }

        //需要进行手机校验，但手机号存在为空现象
        if (isNeedPhoneCheck && (StringUtils.isEmpty(userPhone) || StringUtils.isEmpty(userCheckPhone))){
            return false;
        }

        //需要进行邮箱校验，但邮箱存在为空现象
        if (isNeedMailCheck && (StringUtils.isEmpty(userMail)||StringUtils.isEmpty(userCheckMail))){
            return false;
        }

        //需要进行手机校验,但手机号不一致
        if (isNeedPhoneCheck && !(userPhone.equals(userCheckPhone))){
            return false;
        }

        //需要进行邮箱校验,但邮箱不一致
        if (isNeedMailCheck && !(userMail.equals(userCheckMail))){
            return false;
        }

        return true;
    }

    @Override
    public boolean isAuthComplete(AuthFlow authFlow, SecondLoginInfo secondLoginInfo) {

        if (secondLoginInfo == null){
            return true;
        }

        boolean needMailCheck = secondLoginInfo.getTwoAuthMail()== ConstantType.CONSOLE_ON?true:false;

        boolean needPhoneCheck = secondLoginInfo.getTwoAuthNum()== ConstantType.CONSOLE_ON?true:false;

        boolean needCipherAuthCheck = secondLoginInfo.getTwoAuthSf() == ConstantType.CONSOLE_ON?true:false;

        boolean needBayMaxAuthCheck = secondLoginInfo.getTwoAuthDb() == ConstantType.CONSOLE_ON?true:false;

        boolean needDingTalkAuthCheck = secondLoginInfo.getTwoAuthDd() == ConstantType.CONSOLE_ON?true:false;

        boolean needCipherDynamicCheck = secondLoginInfo.getTwoAuthDt() == ConstantType.CONSOLE_ON?true:false;

        boolean needWeixinCheck = secondLoginInfo.getTwoAuthWx() == ConstantType.CONSOLE_ON?true:false;


        // boolean needDingCodeCheck=secondLoginInfo.getTwoAuthDingDt()==ConstantType.CONSOLE_ON?true:false;

        boolean needDingPushCheck=secondLoginInfo.getTwoAuthDingPush()==ConstantType.CONSOLE_ON?true:false;

        boolean isMailChecked = authFlow == null?false:authFlow.getMail()==ConstantType.CONSOLE_ON?true:false;

        boolean isPhoneChecked = authFlow == null?false:authFlow.getPhoneNumber()==ConstantType.CONSOLE_ON?true:false;

        boolean isCipherChecked = authFlow == null?false:StringUtils.isNotEmpty(authFlow.getSaiFuBindLocalUuid());

        boolean isBayMaxChecked = authFlow == null?false:StringUtils.isNotEmpty(authFlow.getDaBaiBindLocalUuid());

        boolean isDingTalkAuthChecked = authFlow == null?false:StringUtils.isNotEmpty(authFlow.getDingBindLocalUuid());

        boolean isCipherDynamicChecked = authFlow == null?false:StringUtils.isNotEmpty(authFlow.getSaiFuTotpLocalUuid());

      //  boolean isDingCodeCheck=authFlow==null?false:StringUtils.isNotEmpty(authFlow.getDingBindLocalUuid());

        boolean isDingPushCheck=authFlow==null?false:StringUtils.isNotEmpty(authFlow.getDingPushLocalUuid());

        boolean isWeiXinChecked=authFlow==null?false:StringUtils.isNotEmpty(authFlow.getWeixinLocalUuid());


        //需要进行邮箱检查而未进行邮箱校验
        if (needMailCheck && !isMailChecked){
            return false;
        }

        //需要进行手机检查而未进行手机检查
        if (needPhoneCheck && !isPhoneChecked){
            return false;
        }

        //需要进行赛赋扫码认证而未进行赛赋扫码认证
        if (needCipherAuthCheck && !isCipherChecked){
            return false;
        }

        if (needCipherDynamicCheck && !isCipherDynamicChecked){
            return false;
        }

        //需要进行大白认证而未进行大白认证
        if (needBayMaxAuthCheck && !isBayMaxChecked){
            return false;
        }

        //需要进行钉钉认证而未进行钉钉认证
        if (needDingTalkAuthCheck && !isDingTalkAuthChecked){
            return false;
        }
/*
        //需要进行钉钉动态码认证
        if(needDingCodeCheck && !isDingCodeCheck){
            return false;
        }*/

        //需要进行钉钉push认证
        if(needDingPushCheck && !isDingPushCheck){
            return  false;
        }

        //需要微信扫码认证
        if(needWeixinCheck && !isWeiXinChecked){
            return  false;
        }
        return true;
    }

    @Override
    public String obtainCopyright(String companyUuid) {
        return identityModesDAO.selectCopyright(companyUuid);
    }

    @Override
    public IconSettingsDomain getIconSettingsByCompanyUuid(String companyUuid, Integer type) {
        return identityModesDAO.selectIconSettingsByCompanyUuid(companyUuid,type);
    }

}
