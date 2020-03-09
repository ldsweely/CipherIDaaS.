package cipher.console.oidc.service.impl;

import cipher.console.oidc.domain.web.AdminUser;
import cipher.console.oidc.domain.web.CompanyInfoDomain;
import cipher.console.oidc.mapper.AuthStrategyMapper;
import cipher.console.oidc.mapper.CompanyMapper;
import cipher.console.oidc.service.CompanyService;
import cipher.console.oidc.service.IdentityGroupMapService;
import cipher.console.oidc.service.authsettings.AuthStrategyService;
import cipher.console.oidc.service.datainit.DataInitService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 10:25
 */
@Service
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private DataInitService dataInitService;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    AuthStrategyMapper authStrategyMapper;

    @Override
    public String getCompanyUUid(String domain) {
        //入参检验
        if (StringUtils.isEmpty(domain)){
            return "";
        }
        return companyMapper.getCompanyUUid(domain);
    }

    @Override
    public void insertCompanyInfo(AdminUser adminUser) throws Exception{
        if (adminUser ==null){
            return;
        }
        companyMapper.insertCompanyInfo(adminUser);
    }

    @Transactional
    @Override
    public void initData(String companyUUid) {
        dataInitService.sendBaseInfo(companyUUid);
    }

    @Override
    public String getCompanyByUuid(String companyUUid) {
        return companyMapper.getCompanyByUuid(companyUUid);
    }

    void insertIdentity(String companyUUid) throws Exception{
            authStrategyMapper.dataInsert(companyUUid);
    }
}
