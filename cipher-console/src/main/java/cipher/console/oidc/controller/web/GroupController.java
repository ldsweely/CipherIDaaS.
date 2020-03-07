package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.model.GroupApplicationModel;
import cipher.console.oidc.model.GroupInfoModel;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.AdminBehaviorInfoService;
import cipher.console.oidc.service.AppService;
import cipher.console.oidc.service.ApplicationGroupService;
import cipher.console.oidc.service.GroupService;
import cipher.console.oidc.token.CheckToken;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

import static cipher.console.oidc.common.ReturnUtils.failureResponse;
import static cipher.console.oidc.common.ReturnUtils.successResponse;

/**
 * @Author: zt
 * @Date: 2018/6/5 16:15
 */
@Controller
@RequestMapping(value = "/cipher/group")
@EnableAutoConfiguration
public class GroupController {

    static List<GroupInfoDomain> childGroup = new ArrayList<GroupInfoDomain>();


    @Autowired
    private GroupService groupService;

    @Autowired
    private ApplicationGroupService applicationGroupService;


    @Autowired
    private AppService appService;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    @Autowired
    private RedisClient<String,Object> redisClient;

    private static final Logger logger = LoggerFactory.getLogger(GroupController.class.getSimpleName());

    /**
     * 组的下拉选择框
     *
     * @return
     */
    @RequestMapping(value = "/common", params = "json")
    @ResponseBody
    public List<GroupInfoDomain> queryGroupSelect(HttpServletRequest request) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        return groupService.queryGroupSelect(companyId);
    }


    @RequestMapping(value = "/addGroup")
    @ResponseBody
    public Map<String, Object> userlist(@RequestParam(value = "groupId") Integer groupId, HttpServletRequest request, HttpSession session) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        logger.debug("turn to GroupController userlist groupId=[{}],companyId=[{}]", new Object[]{groupId, companyId});
        try {
            Map<String, Object> content = new HashMap<>();
            GroupInfoDomain groupInfoDomain = groupService.getGroupById(groupId);
            redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
            redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));
            List<GroupInfoDomain> groupInfoDomainList = groupService.getAllGroup(companyId);
            content.put("groupId", groupId);
            content.put("groupName", groupInfoDomain.getGroupName());
            content.put("groups", groupInfoDomainList);
            return successResponse("content", content);
        } catch (Exception e) {
            e.printStackTrace();
            return failureResponse("该组不存在!");
        }

    }


    /**
     * 添加部门接口
     * （数据隔离调整）
     */
    @CheckToken
    @RequestMapping(value = "/adding")
    @ResponseBody
    public Map<String, Object> groupAddingSubmit(
            @RequestParam(value = "groupName") String groupName,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "parentGroupId", required = false, defaultValue = "") Integer parentGroupId,
            @RequestParam(value = "companyUUid") String companyId,
            HttpServletRequest request,HttpSession session) {
        if (logger.isDebugEnabled()) {
            logger.debug("turn to /group/adding/submit on the groupAddingSubmit groupName=[{}],description=[{}]",
                    new Object[]{groupName, description});
        }
        redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
        redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));
        Map<String, Object> map = new HashMap<>();
        GroupInfoDomain ss = new GroupInfoDomain();
        ss.setGroupName(groupName);
        ss.setCompanyId(companyId);
        List<GroupInfoDomain> nameGroup = groupService.queryGoupInfoByName(ss);
        if (nameGroup != null && nameGroup.size() > 0) {
            map.put("return_code", 1);
            map.put("return_msg", "组名已存在,请更换组名");
            return map;
        }
        GroupInfoModel groupInfoModel = new GroupInfoModel();
        groupInfoModel.setAccountNumber("");
        groupInfoModel.setGroupName(groupName);
        groupInfoModel.setGroupDescription(description);
        Date currentDate = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        groupInfoModel.setCreateTime(dateFormat.format(currentDate));
        groupInfoModel.setModifyTime(dateFormat.format(currentDate));
        groupInfoModel.setParentGroupId(parentGroupId);
        groupInfoModel.setCompanyId(companyId);
        int result = groupService.insertNewGroup(groupInfoModel);
        if (result > 0) {
            map.put("return_code", 0);
            map.put("return_msg", "添加成功");
        } else {
            map.put("return_code",1);
            map.put("return_msg", "添加失败");
        }

        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.ORIGINATION_UPDATE.getType(),"添加部门:"+groupName);
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return map;
    }

    /**
     * 编辑部门
     *
     * @param groupName     部门名称
     * @param description   部门描述
     * @param parentGroupId 部门父id
     * @param request
     * @return
     * @author cozi
     */
    @RequestMapping(value = "/edited")
    @ResponseBody
    public Map<String, Object> groupEditSubmit(
            @RequestParam(value = "groupId") Integer groupId,
            @RequestParam(value = "groupName") String groupName,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "parentGroupId", required = false, defaultValue = "") Integer parentGroupId,
            HttpServletRequest request,HttpSession session) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
        redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));
        Map<String, Object> map = new HashMap<>();
        GroupInfoDomain groupInfoDomain = new GroupInfoDomain();
        groupInfoDomain.setGroupId(groupId);
        groupInfoDomain.setCompanyId(companyId);
        if (groupId > 0 && StringUtils.isNotEmpty(companyId)) {
            List<GroupInfoDomain> nameGroup = groupService.queryGoupInfoByName(groupInfoDomain);
            if (nameGroup == null || nameGroup.size() <= 0) {
                map.put("return_code", 1);
                map.put("return_msg", "组名不存在");
                return map;
            }
        }
        if (groupId > 0 && StringUtils.isNotEmpty(companyId) && StringUtils.isNotEmpty(groupName)) {
            groupInfoDomain.setGroupName(groupName);
            List<GroupInfoDomain> goupInfoByName = groupService.getGoupInfoByName(groupInfoDomain);
            if (goupInfoByName.size() > 0) {
                map.put("return_code", 1);
                map.put("return_msg", "组名已存在，请更换为其他名称");
                return map;
            }
        }
        //修改部门所在位置
        if (StringUtils.isNotEmpty(groupName)) {
            GroupInfoModel groupInfoModel = new GroupInfoModel();
            groupInfoModel.setGroupId(groupId);
            groupInfoModel.setGroupName(groupName);
            groupInfoModel.setGroupDescription(description);
            groupInfoModel.setParentGroupId(parentGroupId);
            groupInfoModel.setCompanyId(companyId);
            groupService.updateGroup(groupInfoModel);
        }
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.ORIGINATION_UPDATE.getType(),"修改部门:"+groupName);
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        map.put("return_code", 0);
        map.put("return_msg", "编辑成功");
        return map;
    }

    /**
     * 编辑部门回显
     *
     * @param groupId
     * @param request
     * @return
     */
    @RequestMapping(value = "/editEcho")
    @ResponseBody
    public Map<String, Object> editGroupEcho(@Param(value = "groupId") Integer groupId, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        if (groupId > 0) {
            GroupInfoModel groupInfoModel = groupService.geteditGroupEcho(groupId);
            if (groupInfoModel != null) {
                map.put("return_result", groupInfoModel);
                map.put("return_code", 0);
                map.put("return_msg", "组信息查出成功");
                return map;
            }
        }
        map.put("return_code", 1);
        map.put("return_msg", "组信息查出失败！");
        return map;
    }

    /**
     * 编辑部门时，该部门下的子部门不能作为父部门
     *
     * @param
     * @return
     **/
    @RequestMapping(value = "/commons")
    @ResponseBody
    public List<GroupInfoDomain> queryGroupByCompanyId(@Param(value = "groupId") Integer groupId,
                                                       @RequestParam(value = "companyUUid") String companyUUid) {
        //去掉子部门后
        List<GroupInfoDomain> childGroupList = new ArrayList<>();
        if(groupId.intValue()>0){
            List<GroupInfoDomain> groupInfoDomains = groupService.queryGroupSelect(companyUUid);
            //该节点下的子部门
            List<GroupInfoDomain> childGroupInfoDomain = addChildGroup(groupInfoDomains, groupId);
            //加入当前节点
            for(GroupInfoDomain groupInfoDomain:groupInfoDomains){
                if(groupInfoDomain.getGroupId().equals(groupId)){
                    childGroupInfoDomain.add(groupInfoDomain);
                }
            }
            for (GroupInfoDomain groupInfoDomain : groupInfoDomains) {
                if (!childGroupInfoDomain.contains(groupInfoDomain)) {
                    childGroupList.add(groupInfoDomain);
                }
            }
        }
        return childGroupList;
    }



    public List<GroupInfoDomain> addChildGroup(List<GroupInfoDomain> groupInfoDomains, Integer groupId) {
        for (GroupInfoDomain groupInfoDomain : groupInfoDomains) {
            if(groupInfoDomain.getParentGroupId()==null){
                continue;
            }
            if (groupInfoDomain.getParentGroupId().equals(groupId)) {
                addChildGroup(groupInfoDomains, groupInfoDomain.getGroupId());
                childGroup.add(groupInfoDomain);
            }
        }
        return childGroup;
    }

    @RequestMapping(value = "/addAppPre")
    @ResponseBody
    public Map<String, Object> appList(@RequestParam(value = "groupId") String groupId) {
        Map<String, Object> content = new HashMap<>();
        GroupAuthorizationMapDomain record = new GroupAuthorizationMapDomain();
        record.setGroupId(Integer.valueOf(groupId));
        List<ApplicationInfo> appList = applicationGroupService.selectAppList(record);
        if (null != appList && appList.size() > 0) {
            List<String> appIds = new ArrayList<>();
            for (ApplicationInfo domain1 : appList) {
                appIds.add(String.valueOf(domain1.getId()));
            }
            String str = String.join(",", appIds);
            content.put("appList", str);
        }
        content.put("groupId", groupId);
        return successResponse("content", content);
    }

    /*
     *  部门授权接口
     * (数据隔离：该接口未调整)
     *
     * */
    @CheckToken
    @RequestMapping(value = "/addApp", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addApp(GroupAuthorizationMapDomain form,HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        try {
            GroupAuthorizationMapDomain record = new GroupAuthorizationMapDomain();
            record.setGroupId(Integer.valueOf(form.getGroupId()));
            applicationGroupService.deleteGroupAuthorizationMap(record);
            String appList = form.getAppList();
            if (StringUtils.isNotEmpty(appList)) {
                String[] appIds = appList.split(",");
                for (int i = 0; i < appIds.length; i++) {
                    GroupAuthorizationMapDomain domain = new GroupAuthorizationMapDomain();
                    domain.setApplicationId((appIds[i]));
                    domain.setGroupId(form.getGroupId());
                    applicationGroupService.insertGroupAuthorizationMap(domain);
                }
            }
            map.put("return_msg", "操作成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.AUTHORIZED_MAINTENANC.getType(),"部门授权");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        return map;
    }

    /*
     * 点击用户获取部门树
     * （数据隔离修改）
     *
     * */
    @CheckToken
    @RequestMapping(value = "/treeList")
    @ResponseBody
    public List<TreeNodesDomain> treeList(@RequestParam(value = "accountNumber", required = false) String accountNumber,
                                          @RequestParam(value = "uuid") String uuid,
                                          HttpServletRequest request) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        logger.debug("Enter GroupController.treeList param uuid:" + uuid + "companyId:" + companyId);
        List<GroupInfoDomain> groupList = groupService.getGroupNamesById(uuid);
        List<Integer> groupIds = new ArrayList();
        for (GroupInfoDomain groupInfoDomain : groupList) {
            groupIds.add(groupInfoDomain.getGroupId());
        }
        return groupService.getGroupTreeList(groupIds, companyId);
    }


    @RequestMapping(value = "/appList")
    @ResponseBody
    public List<ApplicationInfoDomain> getTeamList(@RequestParam(value = "groupId") String groupId) {
        List<ApplicationInfoDomain> applicationInfoList = appService.queryAllApplicationNameAndId();
        List<GroupApplicationModel> selectHaveGroupList = applicationGroupService.selectHaveGroupList(Integer.valueOf(groupId));
        for (ApplicationInfoDomain applicationInfoDomain : applicationInfoList) {
            applicationInfoDomain.setTeamName(applicationInfoDomain.getApplicationName());
            for (GroupApplicationModel groupApplicationModel : selectHaveGroupList) {
                if (null != groupApplicationModel) {
                    if (applicationInfoDomain.getApplicationId().equals(groupApplicationModel.getApplicationId())) {
                        applicationInfoDomain.setState();
                    }
                }
            }
        }
        return applicationInfoList;
    }

}
