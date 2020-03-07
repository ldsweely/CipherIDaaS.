package com.portal.daoAuthoriza;


import com.portal.domain.DingTalkConfig;
import org.apache.ibatis.annotations.Param;

public interface DingTalkConfigDAO {
    /**
     * 根据公司ID，查询钉钉信息
     * */
    DingTalkConfig getDingTalkConfigByCompanyId(String companyId);

    DingTalkConfig getPortalDingConfig(@Param(value = "companyId") String companyId);
}
