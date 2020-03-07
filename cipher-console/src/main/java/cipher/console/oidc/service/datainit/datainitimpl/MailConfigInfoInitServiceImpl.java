package cipher.console.oidc.service.datainit.datainitimpl;

import cipher.console.oidc.mapper.DataInitMapper;
import cipher.console.oidc.service.datainit.DataInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO:
 * create by shizhao at 2020-3-6
 *
 * @author: shizhao
 * @since: 2020-3-6 16:36
 */
@Service("mailConfigInfoInitServiceImpl")
public class MailConfigInfoInitServiceImpl implements DataInitService {

    @Autowired
    private DataInitMapper dataInitMapper;

    @Override
    public void sendBaseInfo(String companyUuid) {

    }

    @Override
    public void init(String companyUUid) throws Exception {
            dataInitMapper.emailConfigDataInit(companyUUid);
    }
}
