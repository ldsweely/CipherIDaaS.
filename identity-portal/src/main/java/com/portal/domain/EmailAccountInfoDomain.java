package com.portal.domain;


import com.portal.jms.base.BaseMessage;

public class EmailAccountInfoDomain extends BaseMessage {
    private String companyId;
    private String mail;
    private String account;
    private String password;

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

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

    @Override
    public String toString() {
        return "EmailAccountInfoDomain{" +
                "companyId='" + companyId + '\'' +
                ", mail='" + mail + '\'' +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
