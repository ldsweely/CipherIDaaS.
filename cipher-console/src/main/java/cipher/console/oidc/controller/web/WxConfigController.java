package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.NewReturnUtils;
import cipher.console.oidc.domain.web.AdminBehaviorInfo;
import cipher.console.oidc.domain.web.AutoSyncInfo;
import cipher.console.oidc.domain.web.WxConfigDomain;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.model.LocalWxMapModel;
import cipher.console.oidc.service.AdminBehaviorInfoService;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.service.WxConfigService;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 企业微信基本配置
 *
 * @Author: zt
 * @Date: 2019-04-29 14:40
 */
@RestController
@RequestMapping(value = "/cipher/wxConfig")
public class WxConfigController {

    @Autowired
    private WxConfigService wxConfigService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    private Logger logger = LoggerFactory.getLogger(WxConfigController.class);

    /**
     * 增加或编辑基础配置
     *
     * @param response
     * @return
     */
    @RequestMapping(value = "/baseConfig")
    public Map<String, Object> addOrEditConfig(HttpServletResponse response, WxConfigDomain form, HttpSession session) {
        form.setCompanyUUid(sessionService.getCompanyUUid(session));
        AdminBehaviorInfo record = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加或修改企业微信配置信息");
        adminBehaviorInfoService.insertSelective(record,session);
        return wxConfigService.insertOrUpdateBaseConfig(form);
    }

    /**
     * 获取基础配置信息
     *
     * @param response
     * @return
     */
    @RequestMapping(value = "/getBaseConfig")
    public WxConfigDomain getConfig(HttpServletResponse response, HttpSession session) {
        return wxConfigService.queryConfig(sessionService.getCompanyUUid(session));
    }


    /**
     * 新增或编辑属性映射
     *
     * @param localWxMapList
     * @param response
     * @return
     */
    /**
     * 增加企业微信自动同步配置
     * @author cozi
     * @date 2019-08-08s
     * @param localWxMapList
     * @param response
     * @param session
     * @return
     */
    @RequestMapping(value = "/editAttrConfig")
    public Map<String, Object> editAttrConfig(
            @RequestParam(value = "localWxMapList") String localWxMapList,
            AutoSyncInfo autoSyncInfo,
            HttpServletResponse response,
            HttpSession session) {

        if (StringUtils.isEmpty(localWxMapList)) {
            return NewReturnUtils.failureResponse("数据缺失!");
        }

        try {
            JSON.parseArray(localWxMapList, LocalWxMapModel.class);
        } catch (Exception e) {
            logger.error("json data is incorect:" + localWxMapList);
            return NewReturnUtils.failureResponse("数据不正确!");
        }
        WxConfigDomain wxConfigDomain = new WxConfigDomain();
        wxConfigDomain.setCompanyUUid(sessionService.getCompanyUUid(session));
        wxConfigDomain.setAttrMap(localWxMapList);
        //更新映射配置
        wxConfigService.insertOrUpdateBaseConfig(wxConfigDomain);
        //更新自动同步配置
        wxConfigService.updateAutoSync(sessionService.getCompanyUUid(session),new Gson().toJson(autoSyncInfo));
        AdminBehaviorInfo record = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加或修改企业微信属性配置信息");
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
     * 增加企业微信同步属性设置
     * @author cozi
     * @date 2019-08-08
     * @param response
     * @param session
     * @return
     */
    @RequestMapping(value = "/getAttrConfig")
    public Map<String,Object> getAttrConfig(HttpServletResponse response, HttpSession session) {
        String companyId = sessionService.getCompanyUUid(session);
        WxConfigDomain wxConfigDomain = wxConfigService.queryConfig(companyId);
        String autoSync = wxConfigService.queryAutoSync(companyId);
        AutoSyncInfo autoSyncInfo = new Gson().fromJson(autoSync, AutoSyncInfo.class);
        Map<String,Object> map = new HashMap<>();
        if (wxConfigDomain == null || StringUtils.isEmpty(wxConfigDomain.getAttrMap())) {
            return null;
        }
        List<LocalWxMapModel> list = JSON.parseArray(wxConfigDomain.getAttrMap(), LocalWxMapModel.class);
        map.put("localWx",list);
        map.put("autoSyncInfo",autoSyncInfo);
        return map;
    }

    /**
     * @param rule     0-手机号,1-邮箱，2-手机号and邮箱，3-手机号or邮箱
     * @param response
     * @return
     */
    @RequestMapping(value = "/editMatchRule")
    public Map<String, Object> editMatchRule(Integer rule, HttpServletResponse response, HttpSession session) {
        WxConfigDomain wxConfigDomain = new WxConfigDomain();
        wxConfigDomain.setCompanyUUid(sessionService.getCompanyUUid(session));
        wxConfigDomain.setMatchRule(rule);
        wxConfigService.insertOrUpdateBaseConfig(wxConfigDomain);
        AdminBehaviorInfo record = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加或修改企业微信配置规则");
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
        WxConfigDomain wxConfigDomain = wxConfigService.queryConfig(companyId);
        if (wxConfigDomain == null || StringUtils.isEmpty(wxConfigDomain.getAttrMap())) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("rule", wxConfigDomain.getMatchRule());
        return map;
    }

}
