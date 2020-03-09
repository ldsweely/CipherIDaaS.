package cipher.console.oidc.domain.datainit;

import cipher.console.oidc.util.UrlUtils;

import java.net.URL;

/**
 * TODO:
 * create by shizhao at 2020-3-9
 *
 * @author: shizhao
 * @since: 2020-3-9 12:07
 */
public class SystemInfoDataInitDomain {
    private String ip;
    private String deviceName;
    private String dsgServerUrl;
    private String consoleUrl;
    private String consoleRedirectUrl;
    private int expireTime;
    private String imageHost;
    private String portalServerUrl;
    private String casServerAuthUrl;
    private String rdsgServerAuthUrl;

    private String samlLoginUrl;

    private String rdsgServerUrl;

    private String bindingAccountUrl;

    private String redirectPortalUrl;

    private String companyUuid;


    public SystemInfoDataInitDomain(String companyDomain,String companyUuid) {
        String domain= UrlUtils.getDynamicUrl(companyDomain);
        this.ip =domain;
        this.deviceName="portal认证系统";
        this.dsgServerUrl=companyDomain+":9999";
        this.consoleUrl=companyDomain+":7777";
        this.consoleRedirectUrl=companyDomain+":8642";
        this.expireTime=10000;
        this.imageHost=companyDomain;
        this.casServerAuthUrl=companyDomain+ ":8645/authLogic/login/staff/";
        this.rdsgServerAuthUrl =companyDomain+":8645/authLogic/login/staff/";
        this.samlLoginUrl=companyDomain+":8645/cipher/saml/login/";
        this.rdsgServerUrl =companyDomain+":80/sfLogin";
        this.bindingAccountUrl=companyDomain+":8645/skipSubConfig";
        this.redirectPortalUrl=companyDomain+":8645/cipher/oauth/login";
        this.portalServerUrl =companyDomain+":8443";
        this.companyUuid =companyUuid;
    }

    public String getCompanyUuid() {
        return companyUuid;
    }

    public void setCompanyUuid(String companyUuid) {
        this.companyUuid = companyUuid;
    }

    public String getRedirectPortalUrl() {
        return redirectPortalUrl;
    }

    public void setRedirectPortalUrl(String redirectPortalUrl) {
        this.redirectPortalUrl = redirectPortalUrl;
    }

    public String getBindingAccountUrl() {
        return bindingAccountUrl;
    }

    public void setBindingAccountUrl(String bindingAccountUrl) {
        this.bindingAccountUrl = bindingAccountUrl;
    }

    public String getRdsgServerUrl() {
        return rdsgServerUrl;
    }

    public void setRdsgServerUrl(String rdsgServerUrl) {
        this.rdsgServerUrl = rdsgServerUrl;
    }

    public String getSamlLoginUrl() {
        return samlLoginUrl;
    }

    public void setSamlLoginUrl(String samlLoginUrl) {
        this.samlLoginUrl = samlLoginUrl;
    }

    public String getRdsgServerAuthUrl() {
        return rdsgServerAuthUrl;
    }

    public void setRdsgServerAuthUrl(String rdsgServerAuthUrl) {
        this.rdsgServerAuthUrl = rdsgServerAuthUrl;
    }

    public String getCasServerAuthUrl() {
        return casServerAuthUrl;
    }

    public void setCasServerAuthUrl(String casServerAuthUrl) {
        this.casServerAuthUrl = casServerAuthUrl;
    }

    public String getPortalServerUrl() {
        return portalServerUrl;
    }

    public void setPortalServerUrl(String portalServerUrl) {
        this.portalServerUrl = portalServerUrl;
    }

    public String getImageHost() {
        return imageHost;
    }

    public void setImageHost(String imageHost) {
        this.imageHost = imageHost;
    }

    public int getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(int expireTime) {
        this.expireTime = expireTime;
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
}
