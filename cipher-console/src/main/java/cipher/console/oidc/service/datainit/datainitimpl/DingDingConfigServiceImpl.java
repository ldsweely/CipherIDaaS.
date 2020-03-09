package cipher.console.oidc.service.datainit.datainitimpl;

import cipher.console.oidc.domain.DataInitIdDomain;
import cipher.console.oidc.domain.datainit.DingscanInitInfo;
import cipher.console.oidc.mapper.DataInitMapper;
import cipher.console.oidc.mapper.DingConfigMapper;
import cipher.console.oidc.service.datainit.DataInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO:
 * create by shizhao at 2020-3-5
 *
 * @author: shizhao
 * @since: 2020-3-5 17:09
 */
@Service("dingDingConfigServiceImpl")
public class DingDingConfigServiceImpl implements DataInitService {

    @Autowired
    private DataInitMapper dataInitMapper;

    @Override
    public void sendBaseInfo(String companyUuid) {
        this.sendBaseInfo(companyUuid);
    }

    @Override
    public void init(String companyUUid) throws Exception {

        DataInitIdDomain dataInitIdDomain=new DataInitIdDomain(companyUUid);
        //插入钉钉基本 配置
        int configId=dataInitMapper.dingDingConfigInitData(dataInitIdDomain);
        DingscanInitInfo dingscanInitInfo=new DingscanInitInfo(companyUUid,String.valueOf(dataInitIdDomain.getId()));
        //插入钉钉回调配置
        dataInitMapper.dingConfigAppInitData(dingscanInitInfo);
    }
}
