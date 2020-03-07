package com.portal.domain;

public class DingEwmDomain {
    private String dingUserId;
    private String dingUnionId;
    private String userId;
    private String companyId;

    public String getDingUserId() {
        return dingUserId;
    }

    public void setDingUserId(String dingUserId) {
        this.dingUserId = dingUserId;
    }

    public String getDingUnionId() {
        return dingUnionId;
    }

    public void setDingUnionId(String dingUnionId) {
        this.dingUnionId = dingUnionId;
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

    @Override
    public String toString() {
        return "DingEwmDomain{" +
                "dingUserId='" + dingUserId + '\'' +
                ", dingUnionId='" + dingUnionId + '\'' +
                ", userId='" + userId + '\'' +
                ", companyId='" + companyId + '\'' +
                '}';
    }
}
