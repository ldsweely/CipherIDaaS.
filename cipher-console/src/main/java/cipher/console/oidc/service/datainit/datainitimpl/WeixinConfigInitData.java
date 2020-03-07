package cipher.console.oidc.service.datainit.datainitimpl;

import cipher.console.oidc.domain.datainit.DingscanInitInfo;
import cipher.console.oidc.mapper.DataInitMapper;
import cipher.console.oidc.service.datainit.DataInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO:
 * create by shizhao at 2020-3-6
 *
 * @author: shizhao
 * @since: 2020-3-6 14:45
 */
@Service("weiXinConfigServiceImpl")
public class WeixinConfigInitData implements DataInitService {

    @Autowired
    private DataInitMapper dataInitMapper;

    @Override
    public void sendBaseInfo(String companyUuid) {
        this.sendBaseInfo(companyUuid);
    }

    @Override
    public void init(String companyUUid) throws Exception {
        //插入微信基本 配置
        int configId=dataInitMapper.weixinConfigAppInitData(companyUUid);

        //插入微信回调配置
        dataInitMapper.weixinScanConfigAppInitData(companyUUid,String.valueOf(configId));
    }
}
