package cipher.console.oidc.service.impl;

import cipher.console.oidc.common.NewReturnUtils;
import cipher.console.oidc.common.ReturnMsg;
import cipher.console.oidc.domain.web.IconSettingsDomain;
import cipher.console.oidc.mapper.IconSettingsMapper;
import cipher.console.oidc.service.IconSettingsService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
@Service
public class IconSettingsServiceImpl implements IconSettingsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IconSettingsServiceImpl.class);

    @Autowired
    private IconSettingsMapper iconSettingsMapper;

    @Override
    public Map<String, Object> compileDoorPage(String companyUuid, String title, String leftTitle, String rightTitle, String iconfont, String logo) {
            int count = iconSettingsMapper.countByCompanyUuid(companyUuid, 2);
            IconSettingsDomain iconSettingsDomain = new IconSettingsDomain();
            iconSettingsDomain.setCompanyUuid(companyUuid);
            iconSettingsDomain.setType(2);
            iconSettingsDomain.setTitle(title);
            iconSettingsDomain.setLeftTitle(leftTitle);
            iconSettingsDomain.setRightTitle(rightTitle);
            iconSettingsDomain.setIconfont(iconfont);
            iconSettingsDomain.setLogo(logo);
            if (count > 0) {
                try {
                    iconSettingsMapper.updateDoorPage(iconSettingsDomain);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("enter DoorPageServiceImpl.compileDoorPage() but update failed," +
                            "title=[{}],iconfont=[{}],logo=[{}]..==" + new Object[]{companyUuid, title, iconfont, logo});
                    LOGGER.error(e.getMessage(), e);
                    return NewReturnUtils.failureResponse(ReturnMsg.getDoorPageMsg(2));
                }
            } else {
                try {
                    iconSettingsMapper.insertDoorPage(iconSettingsDomain);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("enter DoorPageServiceImpl.compileDoorPage() but insert failed," +
                            "title=[{}],iconfont=[{}],logo=[{}]..==" + new Object[]{companyUuid, title, iconfont, logo});
                    LOGGER.error(e.getMessage(), e);
                    return NewReturnUtils.failureResponse(ReturnMsg.getDoorPageMsg(3));
                }
            }
        return NewReturnUtils.successResponse(ReturnMsg.getDoorPageMsg(0));
    }

    @Override
    public Map<String, Object> echoDoorPage(String companyUuid) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(companyUuid)) {
            IconSettingsDomain iconSettingsDomain = iconSettingsMapper.selectIconSettingsByCompanyUuid(companyUuid, 2);//type为2表示portal登录页
            if (iconSettingsDomain != null) {
                map = NewReturnUtils.successResponse(ReturnMsg.getDoorPageMsg(4));
                map.put("return_result", iconSettingsDomain);
                return map;
            }
        }
        return NewReturnUtils.failureResponse(ReturnMsg.getDoorPageMsg(5));
    }

    @Override
    public Map<String, Object> compileApplicationPage(String companyUuid, String title, String iconfont) {
            int count = iconSettingsMapper.countByCompanyUuid(companyUuid, 3);
            IconSettingsDomain iconSettingsDomain = new IconSettingsDomain();
            iconSettingsDomain.setType(3);
            iconSettingsDomain.setCompanyUuid(companyUuid);
            iconSettingsDomain.setTitle(title);
            iconSettingsDomain.setIconfont(iconfont);
            if (count > 0) {
                try {
                    iconSettingsMapper.updateApplicationPage(iconSettingsDomain);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("enter DoorPageServiceImpl.compileApplicationPage() but update failed," +
                            "title=[{}],iconfont=[{}],..==" + new Object[]{companyUuid, title, iconfont});
                    LOGGER.error(e.getMessage(), e);
                    return NewReturnUtils.failureResponse(ReturnMsg.getApplicationPageMsg(2));
                }
            } else {
                try {
                    iconSettingsMapper.insertApplicationPage(iconSettingsDomain);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("enter DoorPageServiceImpl.compileApplicationPage() but insert failed," +
                            "title=[{}],iconfont=[{}],..==" + new Object[]{companyUuid, title, iconfont});
                    LOGGER.error(e.getMessage(), e);
                    return NewReturnUtils.failureResponse(ReturnMsg.getApplicationPageMsg(3));
                }
            }
        return NewReturnUtils.successResponse(ReturnMsg.getApplicationPageMsg(0));
    }

    @Override
    public Map<String, Object> echoApplicationPage(String companyUuid) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(companyUuid)) {
            IconSettingsDomain iconSettingsDomain = iconSettingsMapper.selectIconSettingsByCompanyUuid(companyUuid, 3);
            if (iconSettingsDomain != null) {
                Map<String, Object> domain = new HashMap<>();
                domain.put("title", iconSettingsDomain.getTitle());
                domain.put("iconfont", iconSettingsDomain.getIconfont());
                map = NewReturnUtils.successResponse(ReturnMsg.getApplicationPageMsg(4));
                map.put("return_result", domain);
                return map;
            }
        }
        return NewReturnUtils.failureResponse(ReturnMsg.getApplicationPageMsg(5));
    }

    @Override
    public Map<String, Object> compileManagePage(String companyUuid, String title, String leftTitle, String rightTitle, String iconfont) {
            int count = iconSettingsMapper.countByCompanyUuid(companyUuid, 1);
            IconSettingsDomain iconSettingsDomain = new IconSettingsDomain();
            iconSettingsDomain.setCompanyUuid(companyUuid);
            iconSettingsDomain.setType(1);
            iconSettingsDomain.setTitle(title);
            iconSettingsDomain.setLeftTitle(leftTitle);
            iconSettingsDomain.setRightTitle(rightTitle);
            iconSettingsDomain.setIconfont(iconfont);
            if (count > 0) {
                try {
                    iconSettingsMapper.updateManagePage(iconSettingsDomain);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("enter DoorPageServiceImpl.compileManagePage() but update failed," +
                            "title=[{}],leftTitle=[{}],rightTitle=[{}],iconfont=[{}],..==" + new Object[]{companyUuid, title, leftTitle, rightTitle, iconfont});
                    LOGGER.error(e.getMessage(), e);
                    return NewReturnUtils.failureResponse(ReturnMsg.getManagePageMsg(2));
                }
            } else {
                try {
                    iconSettingsMapper.insertManagePage(iconSettingsDomain);
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error("enter DoorPageServiceImpl.compileManagePage() but insert failed," +
                            "title=[{}],leftTitle=[{}],rightTitle=[{}],iconfont=[{}],..==" + new Object[]{companyUuid, title, leftTitle, rightTitle, iconfont});
                    LOGGER.error(e.getMessage(), e);
                    return NewReturnUtils.failureResponse(ReturnMsg.getManagePageMsg(3));
                }
            }
        return NewReturnUtils.successResponse(ReturnMsg.getManagePageMsg(0));
    }

    @Override
    public Map<String, Object> echoManagePage(String companyUuid) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(companyUuid)) {
            IconSettingsDomain iconSettingsDomain = iconSettingsMapper.selectIconSettingsByCompanyUuid(companyUuid, 1);
            String copyright = iconSettingsMapper.obtailCopyright(companyUuid);
            if (iconSettingsDomain != null) {
                map = NewReturnUtils.successResponse(ReturnMsg.getManagePageMsg(4));
                map.put("return_result", iconSettingsDomain);
                map.put("copyright",copyright);
                return map;
            }
        }
        return NewReturnUtils.failureResponse(ReturnMsg.getManagePageMsg(5));
    }

    @Override
    public Map<String, Object> compileTitleTag(String companyId,String title, String iconfont) {
        int count = iconSettingsMapper.countByCompanyUuid(companyId, 4);
        IconSettingsDomain iconSettingsDomain = new IconSettingsDomain();
        iconSettingsDomain.setCompanyUuid(companyId);
        iconSettingsDomain.setType(4);
        iconSettingsDomain.setTitle(title);
        iconSettingsDomain.setIconfont(iconfont);
        if(count>0){
            try {
                iconSettingsMapper.updateTitleTag(iconSettingsDomain);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("enter DoorPageServiceImpl.compileTitleTag() but update failed," +
                        "title=[{}],leftTitle=[{}],rightTitle=[{}],iconfont=[{}],..==" + new Object[]{companyId, title,iconfont});
                LOGGER.error(e.getMessage(), e);
                return NewReturnUtils.failureResponse(ReturnMsg.getTitleTag(2));
            }
        }else {
            try {
                iconSettingsMapper.insertTitleTag(iconSettingsDomain);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("enter DoorPageServiceImpl.compileTitleTag() but insert failed," +
                        "title=[{}],leftTitle=[{}],rightTitle=[{}],iconfont=[{}],..==" + new Object[]{companyId, title,iconfont});
                LOGGER.error(e.getMessage(), e);
                return NewReturnUtils.failureResponse(ReturnMsg.getTitleTag(3));
            }
        }
        return NewReturnUtils.successResponse(ReturnMsg.getTitleTag(0));
    }

    @Override
    public Map<String, Object> echoTitleTag(String companyUuid) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(companyUuid)) {
            IconSettingsDomain iconSettingsDomain = iconSettingsMapper.selectIconSettingsByCompanyUuid(companyUuid, 4);
            if (iconSettingsDomain != null) {
                map = NewReturnUtils.successResponse(ReturnMsg.getTitleTag(4));
                map.put("return_result", iconSettingsDomain);
                return map;
            }
        }
        return NewReturnUtils.failureResponse(ReturnMsg.getTitleTag(5));
    }
}
