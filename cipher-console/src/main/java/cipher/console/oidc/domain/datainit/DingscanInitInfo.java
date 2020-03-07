package cipher.console.oidc.domain.datainit;

/**
 * TODO:
 * create by shizhao at 2020-3-6
 *
 * @author: shizhao
 * @since: 2020-3-6 12:35
 */
public class DingscanInitInfo {

    private String companyUUid;

    private String configId;

    public DingscanInitInfo() {
    }

    public DingscanInitInfo(String companyUUid, String configId) {
        this.companyUUid = companyUUid;
        this.configId = configId;
    }


    public String getCompanyUUid() {
        return companyUUid;
    }

    public void setCompanyUUid(String companyUUid) {
        this.companyUUid = companyUUid;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }
}
