package cipher.console.oidc.domain.web;

import cipher.console.oidc.domain.BaseParamDomain;

import java.io.Serializable;

/**
 * Created by 95744 on 2018/8/23.
 */
public class SmsInfoDomain  extends BaseParamDomain{

    private Integer id;
    private String name;
    private String address;
    private String requestParams;
    private String responseParams;
    private int oneTimeValid;
    private int sendTimeout;
    private String describes;
    private int sendTime;
    private String telephone;
    private int intervalTime;
    private int extendTime;
    private int effectiveTime;

    public int getIntervalTime() {
        return intervalTime;
    }

    public void setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
    }

    public int getExtendTime() {
        return extendTime;
    }

    public void setExtendTime(int extendTime) {
        this.extendTime = extendTime;
    }

    public int getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(int effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public String getResponseParams() {
        return responseParams;
    }

    public void setResponseParams(String responseParams) {
        this.responseParams = responseParams;
    }

    public int getOneTimeValid() {
        return oneTimeValid;
    }

    public void setOneTimeValid(int oneTimeValid) {
        this.oneTimeValid = oneTimeValid;
    }

    public int getSendTimeout() {
        return sendTimeout;
    }

    public void setSendTimeout(int sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    public String getDescribes() {
        return describes;
    }

    public void setDescribes(String describes) {
        this.describes = describes;
    }

    public int getSendTime() {
        return sendTime;
    }

    public void setSendTime(int sendTime) {
        this.sendTime = sendTime;
    }
}
