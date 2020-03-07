package com.portal.daoAuthoriza;

import com.portal.domain.RegisterUserInfo;
import org.apache.ibatis.annotations.Param;

public interface CheckUserMapper {
    //通过主账号查询
    RegisterUserInfo userByAccountNumber(@Param(value = "accountNumber") String accountNumber, @Param(value = "companyId") String companyId);
    //通过邮箱查询
    RegisterUserInfo userByMail(@Param(value = "mail") String mail, @Param(value = "companyId") String companyId);
    //通过手机号查询
    RegisterUserInfo userByTelephone(@Param(value = "telephone") String telephone, @Param(value = "companyId") String companyId);

    //通过主账号查询
    RegisterUserInfo updateUserByAccountNumber(@Param(value = "accountNumber") String accountNumber,
                                               @Param(value = "companyId") String companyId,
                                               @Param(value = "uuid") String uuid);
    //通过邮箱查询
    RegisterUserInfo updateUserByMail(@Param(value = "mail") String mail, @Param(value = "companyId") String companyId,
                                      @Param(value = "uuid") String uuid);
    //通过手机号查询
    RegisterUserInfo updateUserByTelephone(@Param(value = "telephone") String telephone, @Param(value = "companyId") String companyId,
                                           @Param(value = "uuid") String uuid);
}
