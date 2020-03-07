package com.portal.domain;

public class PlatEwmDomain {
    private String platId;
    private String userId;
    private String companyId;

    public String getPlatId() {
        return platId;
    }

    public void setPlatId(String platId) {
        this.platId = platId;
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
        return "PlatEwmDomain{" +
                "platId='" + platId + '\'' +
                ", userId='" + userId + '\'' +
                ", companyId='" + companyId + '\'' +
                '}';
    }
}
