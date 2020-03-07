package cipher.console.oidc.service.datainit.datainitimpl;

import cipher.console.oidc.mapper.AuthStrategyMapper;
import cipher.console.oidc.service.datainit.DataInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 18:24
 */
@Service("identityAuthServiceImpl")
public class IdentityAuthServiceImpl implements DataInitService {

    @Autowired
    private AuthStrategyMapper authStrategyMapper;

    @Override
    public void sendBaseInfo(String companyUuid) {
    }

    @Override
    public void init(String companyUUid) throws Exception {
        authStrategyMapper.dataInsert(companyUUid);
    }
}
