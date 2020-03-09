package cipher.console.oidc.domain;

/**
 * TODO:
 * create by shizhao at 2020-3-9
 *
 * @author: shizhao
 * @since: 2020-3-9 10:56
 */
public class DataInitIdDomain {

    private int id;//自增长Id

    private String companyUuid;

    public DataInitIdDomain(String companyUUid) {
        this.companyUuid = companyUUid;
    }

    public String getCompanyUuid() {
        return companyUuid;
    }

    public void setCompanyUuid(String companyUuid) {
        this.companyUuid = companyUuid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
