package com.portal.service.impl;

import com.cipher.china.channel.enums.AdAuthEnum;
import com.cipher.china.channel.utils.LdapAuthUtil;
import com.portal.commons.CacheKey;
import com.portal.config.LdapMultiSourceConfig;
import com.portal.daoAuthoriza.*;
import com.portal.domain.*;
import com.portal.jms.JMSProducer;
import com.portal.jms.JMSType;
import com.portal.publistener.EquipBehaviorPublistener;
import com.portal.redis.RedisClient;
import com.portal.service.GetResultWithSyncService;
import com.portal.service.RadiusPushAuth;
import com.portal.totp.GoogleAuthenticator;
import com.portal.utils.aes.AES;
import com.portal.utils.aes.AesKey;
import org.apache.camel.component.jms.JmsProducer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/**
 * @Author: zt
 * @Date: 2019-08-08 16:51
 */
@Service
public class RadiusPushAuthImpl implements RadiusPushAuth {

    @Autowired
    private VpnConfigDAO vpnConfigDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private AdInfoDAO adInfoDAO;

    @Autowired
    private DynamicPasswordDAO dynamicPasswordDAO;

    @Autowired
    private JMSProducer jmsProducer;

    @Autowired
    private GetResultWithSyncService getResultWithSyncService;

    @Autowired
    private EquipBehaviorPublistener equipBehaviorPublistener;


    private static final Logger logger = LoggerFactory.getLogger(RadiusPushAuthImpl.class);

    @Override
    public String radiusAuth(String userName, String pwd, String ip) {

        VpnConfigDomain configDomain = vpnConfigDAO.getConfigByIp(ip);

        if (configDomain == null) {

            logger.error("未获取到相关的设备信息,vpnIp={}", ip);
            return null;
        }
        //该vpn未启用
        if (VpnConfigDomain.CLOSE.equals(configDomain.getStatus())) {

            logger.info("该vpn未启用，vpnIp={}", ip);
            return null;
        }


        Integer authType = configDomain.getAuthType();

        //Radius为CHAP认证方式，本地策略不为本地认证，则直接拒绝认证请求
        if (!VpnConfigDomain.LOCAL_AUTH.equals(configDomain.getAuthOrigin())
                && StringUtils.isEmpty(pwd)) {
            logger.info("Radius采用CHAP的协议的方式下，本地不是本地认证策略，直接拒绝认证请求，ip={},user={}", ip, userName);
            return null;
        }

        UserInfoDomain userInfo = userDAO.getUserInfo(userName,configDomain.getCompanyId());

        if (userInfo == null) {
            logger.info("该用户不存在,user={}", userName);
            return null;
        }

        String uuid = userInfo.getUuid();
        String localPwd = userDAO.getPwd(uuid);
        if (!StringUtils.isEmpty(localPwd)) {
            localPwd = AES.decryptFromBase64(localPwd, AesKey.AES_KEY);
        }
        //AD来源
        String source = userInfo.getSource();

        String authPwd = null;

        //AD认证
        if (VpnConfigDomain.AD_AUTH.equals(configDomain.getAuthOrigin())) {
            //用户名密码totp
            if (VpnConfigDomain.USER_PWD_TOTP.equals(authType)) {
                String realPwd = getPwd(pwd);
                boolean adResult = adAuth(realPwd, uuid, source);

                //用户名密码验证不通过
                if (!adResult) {
                    try{
                        EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                                userInfo.getCompanyId(),configDomain.getUuid(),0,"AD认证",
                                "使用账号:"+userInfo.getAccountNumber()+"以用户名密码totp方式登录，用户名密码验证不通过");
                        equipBehaviorPublistener.publish(equipBehaviorInfo);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }
                authPwd = localPwd + getUserTotp(uuid);
                try{
                    EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                            userInfo.getCompanyId(),configDomain.getUuid(),0,"AD认证",
                            "使用账号:"+userInfo.getAccountNumber()+"以用户名密码totp方式登录，用户名密码验证通过");
                    equipBehaviorPublistener.publish(equipBehaviorInfo);
                }catch (Exception e){
                    e.printStackTrace();
                }
                //用户密码
            } else if (VpnConfigDomain.USER_PWD.equals(authType)) {
                boolean authResult = adAuth(pwd, uuid, source);
                //用户名密码验证不通过
                if (!authResult) {
                    try{
                        EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                                userInfo.getCompanyId(),configDomain.getUuid(),0,"AD认证",
                                "使用账号:"+userInfo.getAccountNumber()+"以用户名密码方式登录，用户名密码验证不通过");
                        equipBehaviorPublistener.publish(equipBehaviorInfo);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }
                authPwd = pwd;
                try{
                    EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                            userInfo.getCompanyId(),configDomain.getUuid(),0,"AD认证",
                            "使用账号:"+userInfo.getAccountNumber()+"以用户名密码方式登录，用户名密码验证通过");
                    equipBehaviorPublistener.publish(equipBehaviorInfo);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //本地认证
        if (VpnConfigDomain.LOCAL_AUTH.equals(configDomain.getAuthOrigin())) {
            //用户密码totp
            if (VpnConfigDomain.USER_PWD_TOTP.equals(authType)) {
                authPwd = localPwd + getUserTotp(uuid);
                try{
                    EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                            userInfo.getCompanyId(),configDomain.getUuid(),0,"本地认证",
                            "使用账号:"+userInfo.getAccountNumber()+"以用户密码totp方式登录");
                    equipBehaviorPublistener.publish(equipBehaviorInfo);
                }catch (Exception e){
                    e.printStackTrace();
                }
                //用户密码
            } else if (VpnConfigDomain.USER_PWD.equals(authType)) {
                authPwd = localPwd;
                try{
                    EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                            userInfo.getCompanyId(),configDomain.getUuid(),0,"本地认证",
                            "使用账号:"+userInfo.getAccountNumber()+"以用户密码方式登录");
                    equipBehaviorPublistener.publish(equipBehaviorInfo);
                }catch (Exception e){
                    e.printStackTrace();
                }
                //用户totp
            } else if (VpnConfigDomain.USER_TOTP.equals(authType)) {
                authPwd = getUserTotp(uuid);
                try{
                    EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                            userInfo.getCompanyId(),configDomain.getUuid(),0,"本地认证",
                            "使用账号:"+userInfo.getAccountNumber()+"以用户密码方式登录");
                    equipBehaviorPublistener.publish(equipBehaviorInfo);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

        //二次push认证关闭
        if (VpnConfigDomain.CLOSE.equals(configDomain.getTowAuthWay())) {

            return authPwd;
        }

        //push认证到钉钉小程序
        Long timestamp = System.currentTimeMillis();
        DingPushAuthInfoMessage dingPushAuthInfoMessage = new DingPushAuthInfoMessage(uuid, ip, timestamp);
        jmsProducer.sendMessage(dingPushAuthInfoMessage, JMSType.VPN_DEVICE_SERVICE);

        //轮询认证的结果
        boolean dingPushAuthResult = getResultWithSync(uuid, timestamp);

        if (dingPushAuthResult) {
            try{
                EquipBehaviorInfo equipBehaviorInfo = new EquipBehaviorInfo(userInfo.getUuid(),
                        userInfo.getCompanyId(),configDomain.getUuid(),0,"钉钉push认证",
                        "使用账号:"+userInfo.getAccountNumber()+"钉钉push认证成功");
                equipBehaviorPublistener.publish(equipBehaviorInfo);
            }catch (Exception e){
                e.printStackTrace();
            }
            return authPwd;
        }

        return null;
    }

    /**
     * 动态密码认证
     *
     * @param uuid 用户id
     * @param totp 用户输入的动态码
     * @return
     */
    private boolean totpAuth(String uuid, String totp) {

        String seed = dynamicPasswordDAO.getSeed(uuid);
        if (StringUtils.isEmpty(seed)) {
            logger.info("enter RadiusPushAuthImpl.totpAuth , the seed is null");
            return false;
        }
        String key = AES.decryptFromBase64(seed, AesKey.AES_KEY);
        //谷歌认证
        try {
            return new GoogleAuthenticator().authorize(key, Integer.parseInt(totp));

        } catch (Exception e) {
            return false;
        }
    }

    private String getUserTotp(String uuid) {

        String seed = dynamicPasswordDAO.getSeed(uuid);
        if (StringUtils.isEmpty(seed)) {
            logger.info("enter RadiusPushAuthImpl.totpAuth , the seed is null");
            return null;
        }
        String key = AES.decryptFromBase64(seed, AesKey.AES_KEY);
        //谷歌认证
        try {
            int totpPassword = new GoogleAuthenticator().getTotpPassword(key);
            String format = String.format("%06d", totpPassword);
            logger.info("用户:" + uuid + "   获取到的totp是:" + format);
            return format;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 截取动态码
     *
     * @param pwd
     * @return
     */
    private String getTotp(String pwd) {
        int length = pwd.length();
        if (length > 6) {
            String totp = pwd.substring(length - 6, length);
            logger.info("the totp  is " + totp);
            return totp;
        }
        return null;
    }

    /**
     * 截取密码
     *
     * @param pwd
     * @return
     */
    private String getPwd(String pwd) {
        int length = pwd.length();
        if (length > 6) {
            String realPwd = pwd.substring(0, length - 6);
            logger.info("the realPwd is " + realPwd);
            return realPwd;
        }
        return null;
    }

    /**
     * @param radiusPwd 用户输入的密码
     * @param localPwd  本地数据库的密码
     * @return
     */
    private boolean localAuth(String radiusPwd, String localPwd) {

//        return true;
        if (StringUtils.isEmpty(radiusPwd) || StringUtils.isEmpty(localPwd)) {
            return false;
        }

        if (localPwd.equals(AES.encryptToBase64(radiusPwd, AesKey.AES_KEY))) {
            return true;
        }

        return false;
    }

    /**
     * @param radiusPwd 用户输入的密码
     * @param uuid      用户的uuid
     * @param source    AD ip
     * @return
     */
    private boolean adAuth(String radiusPwd, String uuid, String source) {
        if (StringUtils.isEmpty(source)) {
            return false;
        }


        UserInfoDomain userInfo =  userDAO.getUserInfoByUUid(uuid);

        AdInfoDomain form = new AdInfoDomain();
        form.setIp(source);
        AdInfoDomain adInfoDomain = adInfoDAO.queryAdInfo(form);

        AdAuthEnum adAuthEnum = LdapAuthUtil.
                connect(adInfoDomain.getIp(), adInfoDomain.getPort(),
                        userInfo.getAccountNumber(), radiusPwd);

        if (adAuthEnum.getCode() == AdAuthEnum.AUTH_SUCCESS.getCode()) {
            return true;
        }

        return false;
    }


    /**
     * 同步获取钉钉认证的结果
     *
     * @param uuid 用户的主键id
     * @return
     */
    public boolean getResultWithSync(String uuid, Long timestamp) {
        Boolean result = (Boolean) getResultWithSyncService.getResultWithSync(CacheKey.getDingPushAuthResultKey(uuid, timestamp), null);
        if (result == null) {
            return false;
        }
        logger.info("钉钉push认证的结果:{}", result);
        return result;

    }

}
