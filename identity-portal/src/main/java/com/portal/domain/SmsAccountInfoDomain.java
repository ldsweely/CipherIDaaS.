package com.portal.domain;


import com.portal.jms.base.BaseMessage;

public class SmsAccountInfoDomain extends BaseMessage {
    private String account;
    private String password;
    private String telephone;
    private String companyId;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    @Override
    public String toString() {
        return "SmsAccountInfoDomain{" +
                "account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", telephone='" + telephone + '\'' +
                ", companyId='" + companyId + '\'' +
                '}';
    }
}
