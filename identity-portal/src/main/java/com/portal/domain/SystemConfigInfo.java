package com.portal.domain;

import java.io.Serializable;

/**
 * TODO:
 * create by liuying at 2019/8/20
 *
 * @author liuying
 * @since 2019/8/20 14:47
 */
public class SystemConfigInfo implements Serializable {

    private int id;
    private String ip;
    private String deviceName;
    private String dsgServerUrl;
    private String consoleUrl;
    private String consoleRedirectUrl;
    private int expireTime;
    private String redirectPortalUrl;
    private String casServerAuthUrl;

    private String samlLoginUrl;

    private String companyUuid;

    public String getCompanyUuid() {
        return companyUuid;
    }

    public void setCompanyUuid(String companyUuid) {
        this.companyUuid = companyUuid;
    }

    public String getSamlLoginUrl() {
        return samlLoginUrl;
    }

    public void setSamlLoginUrl(String samlLoginUrl) {
        this.samlLoginUrl = samlLoginUrl;
    }

    public String getCasServerAuthUrl() {
        return casServerAuthUrl;
    }

    public void setCasServerAuthUrl(String casServerAuthUrl) {
        this.casServerAuthUrl = casServerAuthUrl;
    }

    public int getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(int expireTime) {
        this.expireTime = expireTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDsgServerUrl() {
        return dsgServerUrl;
    }

    public void setDsgServerUrl(String dsgServerUrl) {
        this.dsgServerUrl = dsgServerUrl;
    }

    public String getConsoleUrl() {
        return consoleUrl;
    }

    public void setConsoleUrl(String consoleUrl) {
        this.consoleUrl = consoleUrl;
    }

    public String getConsoleRedirectUrl() {
        return consoleRedirectUrl;
    }

    public void setConsoleRedirectUrl(String consoleRedirectUrl) {
        this.consoleRedirectUrl = consoleRedirectUrl;
    }

    public String getRedirectPortalUrl() {
        return redirectPortalUrl;
    }

    public void setRedirectPortalUrl(String redirectPortalUrl) {
        this.redirectPortalUrl = redirectPortalUrl;
    }
}
