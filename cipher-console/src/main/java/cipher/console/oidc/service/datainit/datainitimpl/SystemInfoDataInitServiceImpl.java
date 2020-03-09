package cipher.console.oidc.service.datainit.datainitimpl;

import cipher.console.oidc.domain.datainit.SystemInfoDataInitDomain;
import cipher.console.oidc.domain.web.CompanyInfoDomain;
import cipher.console.oidc.mapper.DataInitMapper;
import cipher.console.oidc.service.CompanyService;
import cipher.console.oidc.service.datainit.DataInitService;
import com.alipay.api.domain.CompanyInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO:
 * create by shizhao at 2020-3-9
 *
 * @author: shizhao
 * @since: 2020-3-9 12:05
 */
@Service("systemInfoDataInitServiceImpl")
public class SystemInfoDataInitServiceImpl implements DataInitService {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private DataInitMapper dataInitMapper;

    @Override
    public void sendBaseInfo(String companyUuid) {

    }

    @Override
    public void init(String companyUUid) throws Exception {
        String companyInfoDomain  = companyService.getCompanyByUuid(companyUUid);
        SystemInfoDataInitDomain systemInfoDataInitDomain=new SystemInfoDataInitDomain(companyInfoDomain,companyUUid);
        dataInitMapper.systemInfoDataInit(systemInfoDataInitDomain);
    }
}
