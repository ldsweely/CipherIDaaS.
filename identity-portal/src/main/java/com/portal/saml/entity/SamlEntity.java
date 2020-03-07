package com.portal.saml.entity;

import com.portal.domain.ApplicationInfoDomain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;

/**
 * TODO:
 * create by shizhao at 2019-12-12
 *
 * @author: shizhao
 * @since: 2019-12-12 14:45
 */
public class SamlEntity implements Serializable {
    private String saml; //saml断言

    private HttpServletRequest request;//saml请求

    private HttpServletResponse response;//saml的response

    private String uuid;//用户唯一标识

    private ApplicationInfoDomain applicationInfoDomain;//应用信息

    private String subAccout;//应用的从账号

    private String relayState; //访问资源的地址

    public SamlEntity(String saml, HttpServletRequest request, HttpServletResponse response, String uuid, ApplicationInfoDomain applicationInfoDomain, String subAccout,String relayState) {
        this.relayState=relayState;
        this.saml = saml;
        this.request = request;
        this.response = response;
        this.uuid = uuid;
        this.applicationInfoDomain = applicationInfoDomain;
        this.subAccout = subAccout;
    }

    public String getRelayState() {
        return relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    public String getSubAccout() {
        return subAccout;
    }

    public void setSubAccout(String subAccout) {
        this.subAccout = subAccout;
    }

    public SamlEntity() {
    }

    @Override
    public String toString() {
        return "SamlEntity{" +
                "saml='" + saml + '\'' +
                ", request=" + request +
                ", response=" + response +
                ", uuid='" + uuid + '\'' +
                ", applicationInfoDomain=" + applicationInfoDomain +
                ", subAccout='" + subAccout + '\'' +
                '}';
    }

    public String getSaml() {
        return saml;
    }

    public void setSaml(String saml) {
        this.saml = saml;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ApplicationInfoDomain getApplicationInfoDomain() {
        return applicationInfoDomain;
    }

    public void setApplicationInfoDomain(ApplicationInfoDomain applicationInfoDomain) {
        this.applicationInfoDomain = applicationInfoDomain;
    }
}
