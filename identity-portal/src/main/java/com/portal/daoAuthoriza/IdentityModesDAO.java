package com.portal.daoAuthoriza;

import com.portal.domain.IconSettingsDomain;
import com.portal.domain.IdentitySettingInfoDomain;
import com.portal.domain.PortalConfigDomain;
import org.apache.ibatis.annotations.Param;

/**
 * @Author: TK
 * @Date: 2019/4/28 10:39
 */
public interface IdentityModesDAO {

    /**
     * 通过companyUuid获取该公司的认证方式
     * @param companyUuid  公司uuid
     * @return
     */
     public IdentitySettingInfoDomain getIdentityModesByCompanyUUid(String companyUuid);



    /**
     * 根据公司uuid获取protal页面的配置页面
     * @param companyUuid 公司uuid
     * @return
     */
    PortalConfigDomain getPortalConfig(String companyUuid);

    String selectCopyright(@Param(value = "companyUuid") String companyUuid);

    IconSettingsDomain selectIconSettingsByCompanyUuid(@Param(value = "companyUuid") String companyUuid, @Param(value = "type") Integer type);
}
