package com.portal.domain;

public class DabbyEwmDomain {
    private String userId;
    private String companyId;
    private String linkAccount;
    private String state;

    public DabbyEwmDomain(){
        this.state = "0";
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getLinkAccount() {
        return linkAccount;
    }

    public void setLinkAccount(String linkAccount) {
        this.linkAccount = linkAccount;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "DabbyEwmDomain{" +
                "userId='" + userId + '\'' +
                ", companyId='" + companyId + '\'' +
                ", linkAccount='" + linkAccount + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}
