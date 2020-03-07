package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.NewReturnUtils;
import cipher.console.oidc.domain.web.AdminBehaviorInfo;
import cipher.console.oidc.domain.web.AutoSyncInfo;
import cipher.console.oidc.domain.web.DingConfigDomain;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.model.LocalDingMapModel;
import cipher.console.oidc.service.AdminBehaviorInfoService;
import cipher.console.oidc.service.DingConfigService;
import cipher.console.oidc.service.SessionService;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉基本配置
 *
 * @Author: zt
 * @Date: 2019-04-29 14:40
 */
@RestController
@RequestMapping(value = "/cipher/dingConfig")
public class DingConfigController {

    @Autowired
    private DingConfigService dingConfigService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    private Logger logger = LoggerFactory.getLogger(DingConfigController.class);

    /**
     * 增加或编辑基础配置
     *
     * @param response
     * @return
     */
    @RequestMapping(value = "/baseConfig")
    public Map<String, Object> addOrEditConfig(HttpServletResponse response, DingConfigDomain form, HttpSession session) {
        form.setCompanyUUid(sessionService.getCompanyUUid(session));
        AdminBehaviorInfo record = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加或修改钉钉配置信息");
        adminBehaviorInfoService.insertSelective(record,session);
        return dingConfigService.insertOrUpdateBaseConfig(form);
    }

    /**
     * 获取基础配置信息
     *
     * @param response
     * @return
     */
    @RequestMapping(value = "/getBaseConfig")
    public DingConfigDomain getConfig(HttpServletResponse response, HttpSession session) {
        return dingConfigService.queryConfig(sessionService.getCompanyUUid(session));
    }


    /**
     * 新增或编辑属性映射
     *
     * @param localDingMapList
     * @param response
     * @return
     */
    /**
     * 增加钉钉自动同步配置
     * @author cozi
     * @date 2019-08-08s
     * @param localDingMapList
     * @param autoSyncInfo 配置项
     * @param response
     * @param session
     * @return
     */
    @RequestMapping(value = "/editAttrConfig")
    public Map<String, Object> editAttrConfig(
            @RequestParam(value = "localDingMapList") String localDingMapList,
            AutoSyncInfo autoSyncInfo,
            HttpServletResponse response,
            HttpSession session) {

        if (StringUtils.isEmpty(localDingMapList)) {
            return NewReturnUtils.failureResponse("数据缺失!");
        }

        try {
            JSON.parseArray(localDingMapList, LocalDingMapModel.class);
        } catch (Exception e) {
            logger.error("json data is incorect:" + localDingMapList);
            return NewReturnUtils.failureResponse("数据不正确!");
        }

        DingConfigDomain dingConfigDomain = new DingConfigDomain();
        dingConfigDomain.setCompanyUUid(sessionService.getCompanyUUid(session));
        dingConfigDomain.setAttrMap(localDingMapList);
        dingConfigService.insertOrUpdateBaseConfig(dingConfigDomain);
        dingConfigService.updateAutoSync(sessionService.getCompanyUUid(session),new Gson().toJson(autoSyncInfo));
        AdminBehaviorInfo record = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加或修改钉钉属性配置信息");
        adminBehaviorInfoService.insertSelective(record,session);
        return NewReturnUtils.successResponse();
    }

    /**
     * 获取属性映射信息
     *
     * @param response
     * @return
     */
    /**
     * 增加钉钉同步属性设置
     * @author cozi
     * @date 2019-08-08
     * @param response
     * @param session
     * @return
     */
    @RequestMapping(value = "/getAttrConfig")
    public Map<String,Object> getAttrConfig(HttpServletResponse response, HttpSession session) {
        String companyId = sessionService.getCompanyUUid(session);
        DingConfigDomain dingConfigDomain = dingConfigService.queryConfig(companyId);
        String autoSync = dingConfigService.queryAutoSync(companyId);
        AutoSyncInfo autoSyncInfo = new Gson().fromJson(autoSync, AutoSyncInfo.class);
        Map<String,Object> map = new HashMap<>();
        if (dingConfigDomain == null || StringUtils.isEmpty(dingConfigDomain.getAttrMap())) {
            return null;
        }
        List<LocalDingMapModel> list = JSON.parseArray(dingConfigDomain.getAttrMap(), LocalDingMapModel.class);
        map.put("localDing",list);
        map.put("autoSync",autoSyncInfo);
        return map;
    }

    /**
     * @param rule     0-手机号,1-邮箱，2-手机号and邮箱，3-手机号or邮箱
     * @param response
     * @return
     */
    @RequestMapping(value = "/editMatchRule")
    public Map<String, Object> editMatchRule(Integer rule, HttpServletResponse response, HttpSession session) {
        DingConfigDomain dingConfigDomain = new DingConfigDomain();
        dingConfigDomain.setCompanyUUid(sessionService.getCompanyUUid(session));
        dingConfigDomain.setMatchRule(rule);
        dingConfigService.insertOrUpdateBaseConfig(dingConfigDomain);
        AdminBehaviorInfo record = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加或修改钉钉配置规则");
        adminBehaviorInfoService.insertSelective(record,session);
        return NewReturnUtils.successResponse();
    }

    /**
     * 获取匹配规则
     *
     * @param response
     * @return
     */
    @RequestMapping(value = "/getMatchRule")
    public Map<String, Object> getMatchRule(HttpServletResponse response, HttpSession session) {
        String companyId = sessionService.getCompanyUUid(session);
        DingConfigDomain dingConfigDomain = dingConfigService.queryConfig(companyId);
        if (dingConfigDomain == null || StringUtils.isEmpty(dingConfigDomain.getAttrMap())) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rule", dingConfigDomain.getMatchRule());
        return map;
    }

}
