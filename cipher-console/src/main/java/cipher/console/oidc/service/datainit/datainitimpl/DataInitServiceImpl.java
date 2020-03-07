package cipher.console.oidc.service.datainit.datainitimpl;

import cipher.console.oidc.enums.DataInitEnum;
import cipher.console.oidc.service.datainit.DataInitService;
import cipher.console.oidc.util.SpringContextUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 17:48
 */
@Service(value = "dataInitService")
public class DataInitServiceImpl implements DataInitService {

    /**
     * 初始化数据循环初始化，运用策略模式
     * @param companyUuid
     */
    @Transactional
    @Override
    public void sendBaseInfo(String companyUuid) {
        //通过appid获取应用的信息并判断应用是否存在
        try {
            for (DataInitEnum dataInitEnum :DataInitEnum.values()){
                DataInitService dataInitService = (DataInitService) SpringContextUtil.getBean(dataInitEnum.getImpl());
                dataInitService.init(companyUuid);
            }
        } catch (Exception e) {
            e.printStackTrace();
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    @Override
    public void init(String companyUUid) {

    }

}
