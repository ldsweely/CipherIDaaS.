package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.domain.authsettingsdomain.SecondAuthStrategyDomain;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.service.*;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.ResponseUtils;
import com.google.gson.Gson;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * 获取身份认证策略接口
 *
 * */
@Controller
@RequestMapping("/cipher/identityStrategy")
public class IdentityStrategyController {

    private static final Logger logger = LoggerFactory.getLogger(IdentityStrategyController.class.getSimpleName());

    @Autowired
    private IdentityStrategyService identityStrategyService;

    @Autowired
    private IdentityGroupMapService identityGroupMapService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;


    /*
     * 获取身份认证策略列表
     * （数据隔离修改）
     *
     * */
    @CheckToken
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> list(HttpServletRequest request, IdentityStrategyDomain identityStrategyDomain, DataGridModel pageModel) {
        String companyUUid = sessionService.getCompanyUUid(request.getSession());

        identityStrategyDomain = (identityStrategyDomain == null ? new IdentityStrategyDomain() : identityStrategyDomain);
        identityStrategyDomain.setCompanyUuid(companyUUid);
        identityStrategyDomain.setPageData(pageModel);
        return identityStrategyService.queryIdentityStrategyList(identityStrategyDomain);
    }


    @RequestMapping(value = "/updatePre")
    @ResponseBody
    public Map<String, Object> update(Model model, Integer id, HttpServletRequest request) {
        String companyId = sessionService.getCompanyUUid(request.getSession());
        Map<String, Object> map = new HashMap<>();
        IdentityStrategyDomain identityStrategyDomain = new IdentityStrategyDomain();
        identityStrategyDomain.setId(id);
        identityStrategyDomain.setCompanyUuid(companyId);
        identityStrategyDomain = identityStrategyService.queryIdentityStrategyById(identityStrategyDomain);
        if (identityStrategyDomain != null) {
            Gson gson = new Gson();
            SecondAuthStrategyDomain secondAuthStrategyDomain = gson.fromJson(identityStrategyDomain.getSecondAuthType().toString(), SecondAuthStrategyDomain.class);
            identityStrategyDomain.setSecondAuthType(secondAuthStrategyDomain);
            IdentityGroupMapDomain domain = new IdentityGroupMapDomain();
            domain.setIdentityId(id);
            List<IdentityGroupMapDomain> list = identityGroupMapService.selectIdentityGroupMap(domain);
            List<TreeNodesDomain> trees = groupService.getNodeTree(list, companyId);
            List<GroupInfoDomain> groupList = new ArrayList<>();
            for (IdentityGroupMapDomain group : list     //把identitygroupMapDomain转换为groupInfoDomain  List对象
            ) {
                GroupInfoDomain groupInfoDomain = new GroupInfoDomain();
                groupInfoDomain.setGroupId(group.getGroupId());
                groupInfoDomain.setGroupName(group.getGroupName());
                groupList.add(groupInfoDomain);
            }
            groupList = groupService.getPath(trees, groupList);//初始化group的上级根路径groupId的拼接以及初始化根路径的groupName的拼接

            map.put("code", 0);
            map.put("identityObj", identityStrategyDomain);
            map.put("groupList", groupList);
            map.put("trees", trees);
            return map;
        }
        map.put("code", 1);
        map.put("identityObj", "");
        map.put("groupList", "");
        map.put("trees", "");
        return map;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ResponseBody
    public void update(HttpServletResponse response, HttpServletRequest request, IdentityStrategyDomain identityStrategyDomain, SecondAuthStrategyDomain secondAuthStrategyDomain,
                       HttpSession session) {
        int strategyName = identityStrategyService.queryCountByStrategyName(identityStrategyDomain.getId(), identityStrategyDomain.getStrategyName());
        if(strategyName>0){
            ResponseUtils.customFailueResponse(response, "认证策略名称已存在！");
            return;
        }
        int priority = identityStrategyService.queryCountByPriority(identityStrategyDomain.getId(), identityStrategyDomain.getPriority());
        if(priority>0){
            ResponseUtils.customFailueResponse(response, "认证策略优先级不能重复！");
            return;
        }
        identityGroupMapService.deleteIdentityGroupMap(identityStrategyDomain.getId());//删除之前的生效范围
        String groupIds = identityStrategyDomain.getGroupIds();
        if (StringUtils.isNotEmpty(groupIds)) {
            String[] groups = groupIds.split(",");
            for (int i = 0; i < groups.length; i++) {
                IdentityGroupMapDomain domain = new IdentityGroupMapDomain();
                domain.setGroupId(Integer.valueOf(groups[i]));
                domain.setIdentityId(identityStrategyDomain.getId());
                identityGroupMapService.insertIdentityGroupMap(domain);
            }
        }
        String companyUUid = sessionService.getCompanyUUid(request.getSession());
        identityStrategyDomain.setCompanyUuid(companyUUid);
        Gson gson = new Gson();
        identityStrategyDomain.setSecondAuthType(gson.toJson(secondAuthStrategyDomain));
        identityStrategyService.modifyIdentityStrategy(identityStrategyDomain);
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.STRATEGY_MAINTENANCE.getType(), "更新身份认证策略");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
        ResponseUtils.customSuccessResponse(response, "修改身份认证策略成功！");
    }


    /*
     * 身份认证添加接口
     * （数据隔离修改）
     *
     * */
    @CheckToken
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public void add(HttpServletResponse response, HttpServletRequest request, IdentityStrategyDomain identityStrategyDomain,
                    SecondAuthStrategyDomain secondAuthStrategyDomain, HttpSession session) {
        String companyUUid = sessionService.getCompanyUUid(request.getSession());
        identityStrategyDomain.setCompanyUuid(companyUUid);
        IdentityStrategyDomain record = identityStrategyService.queryIdentityStrategyById(identityStrategyDomain);
        if (secondAuthStrategyDomain != null && StringUtils.isNotEmpty(companyUUid)) {
            Gson gson = new Gson();
            identityStrategyDomain.setCompanyUuid(companyUUid);
            identityStrategyDomain.setSecondAuthType(gson.toJson(secondAuthStrategyDomain));
        }
        if (null == record) {
            identityStrategyService.saveIdentityStrategy(identityStrategyDomain);
            String groupIds = identityStrategyDomain.getGroupIds();
            if (StringUtils.isNotEmpty(groupIds)) {
                String[] groups = groupIds.split(",");
                for (int i = 0; i < groups.length; i++) {
                    IdentityGroupMapDomain domain = new IdentityGroupMapDomain();
                    domain.setGroupId(Integer.valueOf(groups[i]));
                    domain.setIdentityId(identityStrategyDomain.getId());
                    identityGroupMapService.insertIdentityGroupMap(domain);
                    System.out.println(groups[i]);
                }
            }
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.STRATEGY_MAINTENANCE.getType(), "添加身份认证策略");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
            ResponseUtils.customSuccessResponse(response, "添加身份认证策略成功！");
        } else {
            if (record.getStrategyName().equals(identityStrategyDomain.getStrategyName())) {
                ResponseUtils.customFailueResponse(response, "认证策略名称已存在！");
            } else if (record.getPriority().equals(identityStrategyDomain.getPriority())) {
                ResponseUtils.customFailueResponse(response, "认证策略优先级不能重复！");
            }
        }

    }

    /*
     * 身份认证删除接口
     * （数据隔离修改）
     *
     * */
    @CheckToken
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public void delete(HttpServletResponse response, Integer id, HttpSession session) {
        try {
            IdentityStrategyDomain domain = new IdentityStrategyDomain();
            domain.setId(id);
            identityStrategyService.deleteIdentityStrategy(domain);
            identityGroupMapService.deleteIdentityGroupMap(id);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.STRATEGY_MAINTENANCE.getType(), "删除身份认证策略");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
            ResponseUtils.customSuccessResponse(response, "删除身份认证策略成功！");
        } catch (Exception e) {
            e.getMessage();
            ResponseUtils.customFailueResponse(response, "删除身份认证策略失败！");
        }
    }

    /**
     * create by 田扛
     * create time 2019年3月13日11:24:53
     * 获取部门树结构
     *
     * @return （数据隔离修改）
     */
    @CheckToken
    @RequestMapping(value = "/group")
    @ResponseBody
    public Map<String, Object> groupList(HttpServletRequest request) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        return groupService.getGroupTree(companyId);

    }


}
