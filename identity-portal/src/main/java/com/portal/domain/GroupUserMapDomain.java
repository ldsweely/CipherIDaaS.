package com.portal.domain;

public class GroupUserMapDomain {

    private Integer id;

    private String userId;

    /**
     * 组的id
     */
    private Integer groupId;

    /**
     * 主账号
     */
    private String accountNumber;


    private String groupIds;


    public GroupUserMapDomain() {
    }

    public GroupUserMapDomain(Integer groupId, String userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(String groupIds) {
        this.groupIds = groupIds;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber == null ? null : accountNumber.trim();
    }
}
