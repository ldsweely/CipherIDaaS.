package cipher.console.oidc.service;

import cipher.console.oidc.domain.web.AdminUser;
import cipher.console.oidc.domain.web.CompanyInfoDomain;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 10:22
 */
public interface CompanyService {

    /**
     * 根据域名获取企业companyUUid
     * @param domain
     * @return
     */
    String getCompanyUUid(String domain);

    void insertCompanyInfo(AdminUser adminUser)throws Exception;

    void initData(String companyUUid);

    String getCompanyByUuid(String companyUUid);
}
