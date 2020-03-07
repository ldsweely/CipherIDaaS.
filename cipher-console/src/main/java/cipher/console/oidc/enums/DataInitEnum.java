package cipher.console.oidc.enums;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 17:56
 */
public enum  DataInitEnum {

    IDENTITY_AUTH("identityAuth", "identityAuthServiceImpl"),

    DING_DING_CONFIG("dingdingconfig", "dingDingConfigServiceImpl"),

    WEI_XIN_CONFIG("weiXinConfig", "weiXinConfigServiceImpl"),

    LOGIN_FAILD_STRATEGY("loginFaildStrasegy", "loginFaildStrategyServiceImpl"),

    PASSWORD_SETTING_INFO("passwordSettingInfo", "passwordSettingInfoServiceImpl"),

    IDENTITY_SETTING_INFO("identitySettingInfo", "identitySettingInfoServiceImpl"),

    SMS_CHANNEL_INFO("smsConfigInfo", "smsChannelDataInitServiceImpl"),

    MAIL_CONFIG_INFO("mailConfigInfo", "mailConfigInfoInitServiceImpl"),

    ERP_CONFIG_INFO("erpConfigInfo", "erpDataInitServiceImpl"),

    DOOR_CONFIG_SETTING("doorConfigSetting","doorPageEchoDataInitServiceImpl"),



    ;

    DataInitEnum(String type, String impl) {
        this.type = type;
        this.impl = impl;
    }
    public static String getDataInitEnum(String type) {
        for (DataInitEnum dataInitEnum : DataInitEnum.values()) {
            if (type.equals(dataInitEnum.getType())) {
                return dataInitEnum.impl;
            }
        }
        return null;
    } private String type;

    private String impl;


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImpl() {
        return impl;
    }

    public void setImpl(String impl) {
        this.impl = impl;
    }


}
