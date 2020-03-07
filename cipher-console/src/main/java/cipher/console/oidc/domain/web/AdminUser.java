package cipher.console.oidc.domain.web;

/**
 * TODO:
 * create by shizhao at 2020-2-27
 *
 * @author: shizhao
 * @since: 2020-2-27 15:24
 */
public class AdminUser {

    private String companyUuid; //公司uuid

    private String uuid; //用户uuid

    private String user; //用户账号

    private String email; //用户邮箱

    private String userName; //用户姓名

    private String phone; //用户手机号

    private String isAdmin; //是否是管理员

    private String companyDomain;//公司域名

    public String getCompanyDomain() {
        return companyDomain;
    }

    public void setCompanyDomain(String companyDomain) {
        this.companyDomain = companyDomain;
    }

    public String getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(String isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getCompanyUuid() {
        return companyUuid;
    }

    public void setCompanyUuid(String companyUuid) {
        this.companyUuid = companyUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public AdminUser() {
    }

    @Override
    public String toString() {
        return "AdminUser{" +
                "companyUuid='" + companyUuid + '\'' +
                ", uuid='" + uuid + '\'' +
                ", user='" + user + '\'' +
                ", email='" + email + '\'' +
                ", userName='" + userName + '\'' +
                ", phone='" + phone + '\'' +
                ", isAdmin='" + isAdmin + '\'' +
                ", companyDomain='" + companyDomain + '\'' +
                '}';
    }
}
