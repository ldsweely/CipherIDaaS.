package com.portal.service;

public interface CheckUserService {
    /**
     * 添加用户时，查找主账号，邮箱，手机号是否发生重复
     * @param companyId
     * @param accountNumber
     * @param mail
     * @param telephone
     * @return
     */
    public Integer checkUserInfo(String companyId, String accountNumber, String mail, String telephone);

    /**
     * 修改用户时，查找主账号，邮箱，手机号是否发生重复
     * @param companyId
     * @param uuid
     * @param accountNumber
     * @param mail
     * @param telephone
     * @return
     */
    public Integer checkUserInfoUpdate(String companyId, String uuid, String accountNumber, String mail, String telephone);
}
