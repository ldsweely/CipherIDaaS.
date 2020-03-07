package cipher.console.oidc.service.datainit;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 17:45
 */
public interface DataInitService {
    /**
     * 判断应用信息
     *
     */
    public void sendBaseInfo(String companyUuid);



    /**
     * 应用重定向地址
     */
    public void init(String companyUUid) throws Exception;

}
