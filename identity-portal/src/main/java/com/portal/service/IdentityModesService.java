package com.portal.service;


import com.portal.domain.*;

/**
 * @Author: TK
 * @Date: 2019/4/28 10:10
 */
public interface IdentityModesService {

    IdentitySettingInfoDomain getIdentityModesByCompanyUUid(String companyUuid);

    PortalConfigDomain getPortalConfig(String companyUuid);

    IdentityAuthentication getIdentityAuthenticationByCompanyId(String companyId);

    AccountBinding getAccountBindingByCompanyId(String companyId);

    ForgetPassword getForgetPasswordFlow(String companyId);

    AccountRegistration getAccountRegistration(String companyId);

    ForgetPassword getForgetPassword(String companyId);

    boolean isBindingAuthComplete(UserInfoDomain userInfo,AccountBinding accountBinding,int on);

    boolean isAuthComplete(AuthFlow authFlow,SecondLoginInfo secondLoginInfo);

    String obtainCopyright(String companyUuid);

    IconSettingsDomain getIconSettingsByCompanyUuid(String companyUuid,Integer type);
}
