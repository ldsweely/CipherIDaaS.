package cipher.console.oidc.mapper;

import cipher.console.oidc.domain.web.AdminUser;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 10:25
 */
public interface CompanyMapper {
    String getCompanyUUid(String domain);

    void insertCompanyInfo(AdminUser adminUser) throws Exception;
}
