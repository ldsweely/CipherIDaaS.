package cipher.console.oidc.mapper;

import cipher.console.oidc.domain.web.IconSettingsDomain;
import org.apache.ibatis.annotations.Param;

public interface IconSettingsMapper {
    int countByCompanyUuid(@Param(value = "companyUuid") String companyUuid, @Param(value = "type") Integer type);
    IconSettingsDomain selectIconSettingsByCompanyUuid(@Param(value = "companyUuid") String companyUuid,@Param(value = "type") Integer type);

    void insertDoorPage(IconSettingsDomain iconSettingsDomain) throws Exception;
    void updateDoorPage(IconSettingsDomain iconSettingsDomain) throws Exception;

    void insertApplicationPage(IconSettingsDomain iconSettingsDomain) throws Exception;
    void updateApplicationPage(IconSettingsDomain iconSettingsDomain) throws Exception;

    void insertManagePage(IconSettingsDomain iconSettingsDomain) throws Exception;
    void updateManagePage(IconSettingsDomain iconSettingsDomain) throws Exception;

    void insertTitleTag(IconSettingsDomain iconSettingsDomain) throws Exception;
    void updateTitleTag(IconSettingsDomain iconSettingsDomain) throws Exception;

    String obtailCopyright(@Param(value = "companyUuid") String companyUuid);

}
