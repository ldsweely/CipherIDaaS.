package cipher.console.oidc.mapper;

import cipher.console.oidc.domain.authsettingsdomain.AuthStrategyDomain;
import cipher.console.oidc.domain.authsettingsdomain.IdentitySettingInfo;

public interface AuthStrategyMapper {
    void insertAuthStrategy(AuthStrategyDomain authStrategyDomain) throws Exception;
    void updateAuthStrategy(AuthStrategyDomain authStrategyDomain) throws Exception;
    AuthStrategyDomain selectAuthStrategyByUuid(String companyUuid);

    void dataInsert(String companyUUid) throws Exception;
}
