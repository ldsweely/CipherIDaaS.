package cipher.console.oidc.mapper;

import cipher.console.oidc.domain.datainit.DingscanInitInfo;
import org.apache.ibatis.annotations.Param;

/**
 * TODO:
 * create by shizhao at 2020-3-5
 *
 * @author: shizhao
 * @since: 2020-3-5 17:12
 */
public interface DataInitMapper {

    int dingDingConfigInitData(String companyUUid) throws Exception;

    void dingConfigAppInitData(DingscanInitInfo dingscanInitInfo) throws Exception;

    int weixinConfigAppInitData(String companyUUid) throws Exception;

    void weixinScanConfigAppInitData(@Param("comapanyUUid")String companyUUid,@Param("configId")String configId) throws Exception;

    void loginFaildDataInit(String companyUUid) throws Exception;

    void passwordSettingInfoDataInit(String companyUUid) throws Exception;

    void identitySettingInfoServiceImpl(String companyUUid) throws Exception;

    void smsChannelDataInit(String companyUUid) throws Exception;

    void emailConfigDataInit(String companyUUid) throws Exception;

    void erpDataInit(String companyUUid) throws Exception;

    void doorPageEchoDataInit(String companyUUid) throws Exception;
}
