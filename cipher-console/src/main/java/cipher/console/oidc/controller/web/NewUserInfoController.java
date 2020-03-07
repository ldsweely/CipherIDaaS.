package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstansUMP;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.model.GroupApplicationModel;
import cipher.console.oidc.model.GroupInfoModel;
import cipher.console.oidc.model.UserInfoModel;
import cipher.console.oidc.publistener.UserBehaviorPublistener;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.*;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.IpUtil;
import cipher.console.oidc.util.ResponseUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

import static cipher.console.oidc.common.ReturnUtils.successResponse;

/**
 * Created by 95744 on 2018/9/25.
 */
@Controller
@RequestMapping(value = "/cipher/newUser")
@EnableAutoConfiguration
public class NewUserInfoController {

    private static final Logger logger = LoggerFactory.getLogger(NewUserInfoController.class.getSimpleName());
    @Autowired
    private UserBehaviorInfoService userBehaviorInfoService;
    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    @Autowired
    private NewUserService newUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private OrganitionTreeSerive organitionTreeSerive;

    @Autowired
    private RoleGroupMapService roleGroupMapService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private UserGroupService userGroupService;
    @Autowired
    private ApplicationGroupService applicationGroupService;

    @Autowired
    private TeamUserMapInfoService teamUserMapInfoService;

    @Autowired
    private UserBehaviorPublistener userBehaviorPublistener;

    @Autowired
    private RedisClient<String,Object> redisClient;


    /*
    *
    * 该接口已废弃不用
    * */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Map<String,Object> appPage(@RequestParam(value = "groupId", required = false) String groupId,@RequestParam(value = "accountNumber",required = false)
                             String accountNumber,HttpServletRequest request) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        Map<String,Object> content=new HashMap<>();
        GroupInfoDomain groupInfoDomain = new GroupInfoDomain();
        if (StringUtils.isEmpty(groupId)) {
            groupInfoDomain = groupService.getGroupByParentId(0);
            if (null != groupInfoDomain && !StringUtils.isEmpty(String.valueOf(groupInfoDomain.getGroupId()))) {
                content.put("groupId", null);
                content.put("groupName", groupInfoDomain.getGroupName());
            }
        } else if(groupId.equals("0")) {
            content.put("queryType", "1");
            List<TreeNodesDomain> urlList=getUrlList(1);
            content.put("urlList", urlList);
        }else {
            groupInfoDomain = groupService.getGroupById(Integer.valueOf(groupId));
            List<TreeNodesDomain> tree = organitionTreeSerive.getGroupList(companyId);
            List<TreeNodesDomain> groupTrees = new ArrayList<TreeNodesDomain>();
            groupService.getGroupStruct(groupTrees, tree, Integer.valueOf(groupId));
            Collections.reverse(groupTrees);
            content.put("groupId", groupInfoDomain.getGroupId());
            content.put("groupList", groupTrees);

        }
        if (StringUtils.isNotBlank(groupId)) {
            List<RoleGroupMapDomain> userHaveRole = roleGroupMapService.selectHaveRoleGroupList(Integer.valueOf(groupId));
            String roleNames = "";
            List<String> roleNameList = new ArrayList<>();
            for (RoleGroupMapDomain roleGroupMapDomain : userHaveRole) {
                roleNameList.add(roleGroupMapDomain.getRoleName());
            }
            List<GroupApplicationModel> appHaveList=applicationGroupService.selectHaveGroupList(Integer.valueOf(groupId));
           if(null!=appHaveList&&appHaveList.size()>0){
               for(GroupApplicationModel groupApplicationModel:appHaveList){
                   if(null!=groupApplicationModel&&StringUtils.isNotEmpty(groupApplicationModel.getApplicationName())){
                       roleNameList.add(groupApplicationModel.getApplicationName());
                   }
               }
           }
            roleNames = org.apache.commons.lang3.StringUtils.join(roleNameList, ",");
            content.put("roleNames", roleNames);
        }
        content.put("accountNumber", accountNumber);
        return successResponse("content",content);
    }

    /*
    * 获取该公司下的所有人员信息
    * (数据隔离调整)
    * */
   // @CheckToken
    @RequestMapping(value = "/getlist", params = "json")
    @ResponseBody
    public Map<String, Object> queryData(@RequestParam(value = "companyUUid") String companyUUid,HttpServletRequest request, DataGridModel pageModel, NewUserInfo form) {
        form.setCompanyId(companyUUid);
        if (logger.isDebugEnabled()) {
            logger.debug("turn to add on the NewUserInfoController.queryData, form", new Object[]{form.toString()});
        }
        return newUserService.getNewUserList(form, pageModel);
    }

    /*
    *
    * 点击部门获取用户列表
    * (数据隔离调整)
    * */
   // @CheckToken
    @RequestMapping(value = "/userlist", params = "json")
    @ResponseBody
    public Map<String, Object> userlist(HttpServletResponse response, DataGridModel pageModel, NewUserInfo form) {
        if (logger.isDebugEnabled()) {
            logger.debug("turn to add on the NewUserInfoController.userlist, form", new Object[]{form.toString()});
        }
        return newUserService.getUserListByGroupId(form, pageModel);
    }


   /*
   * 修改部门名称
   *
   * */
    @RequestMapping(value = "/groupEditName")
    @ResponseBody
    public Map<String, Object> groupEditName(HttpServletRequest request,
                                             @RequestParam(value = "groupId") int groupId,
                                             @RequestParam(value = "groupName") String groupName,
                                             @RequestParam(value = "description") String groupDescription) {
        if (logger.isDebugEnabled()) {
        logger.debug("turn to add on the NewUserInfoController.groupEditName, groupId=[{}] groupName=[{}] description=[{}]", new Object[]{groupId,groupName,groupDescription});
       }
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
        redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));
        Map<String, Object> map = new HashMap<>();
        GroupInfoDomain ss = new GroupInfoDomain();
        ss.setGroupName(groupName);
        ss.setGroupDescription(groupDescription);
        ss.setCompanyId(companyId);
        List<GroupInfoDomain> nameGroup = groupService.queryGoupInfoByName(ss);
        if (nameGroup != null && nameGroup.size() > 0) {
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            map.put("msg", "组名已存在,请更换组名");
            return map;
        }
        if (StringUtils.isNotBlank(groupName)) {
            groupService.updateGroupName(new GroupInfoModel(groupName, groupId));
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
            map.put("msg", "操作成功");
        } else {
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            map.put("msg", "操作失败");
        }
        return map;
    }


    /*
    * 删除部门接口
    * */
    @RequestMapping(value = "/groupDelete")
    @ResponseBody
    public Map<String, Object> groupDelete(HttpSession session,HttpServletRequest request,
                                           @RequestParam(value = "groupId") int groupId) {
        if (logger.isDebugEnabled()) {
            logger.debug("turn to add on the NewUserInfoController.groupDelete, groupId=[{}]", new Object[]{groupId});
        }
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
        redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));

        Map<String, Object> map = new HashMap<>();
        int countMembers = groupService.countGroupMembers(groupId);
        int countGroups = groupService.countChildGroup(groupId);
        if (countMembers + countGroups != 0) {
            map.put("code", 0);
            map.put("message", "无法删除该部门，您必须先删除该部门下所有信息");
        } else {
            map.put("code", 1);
            try {
                groupService.deleteGroup(groupId);
                map.put("message", "删除成功!");
                AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.ORIGINATION_UPDATE.getType(), "删除部门成功");
                adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
            } catch (Exception e) {
                map.put("message", "无法删除该部门,请检查该部门是否存在");
                e.printStackTrace();
            }
        }
        return map;
    }



    /*
    * 删除用户接口
    * 已改
    * (数据隔离调整)
    * */
    @CheckToken
    @RequestMapping(value = "/delete")
    @ResponseBody
    public Map<String, Object> delete(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "accountNumber",required = false) String accountNumber,
                                      @RequestParam(value = "uuid") String uuid, HttpSession session) {
         String companyId=ConstantsCMP.getSessionCompanyId(request);
        redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
        redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));

        if (logger.isDebugEnabled()) {
            logger.debug("turn to add on the  NewUserInfoController.delete, uuid=[{}]", new Object[]{uuid});
        }
        Map<String, Object> map = new HashMap<>();
        UserInfoDomain user = userService.getUserInfo(uuid);
        if (null != user && user.getAccountStatus().equals("启用")) {
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            map.put("msg", "账号启用状态不允许删除");
            return map;
        }
        try {

            newUserService.reallyDelete(uuid);

            try {

                //管理员日志
                AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.ACCOUNT_MAINTENANCE.getType(),  "删除用户:"+user.getUserName());
                adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
            }catch (Exception e){
                e.printStackTrace();
            }
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
            map.put("msg", "删除成功");

        } catch (Exception e) {
            e.printStackTrace();
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            map.put("msg", "服务器错误");
        }
        return map;


    }




    /*
    * 账号解锁接口
    * TODO(还需调整)
    * */
    @RequestMapping(value = "/unlock")
    @ResponseBody
    public void unlock(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "accountNumber",required = false) String accountNumber,
                       @RequestParam(value = "uuid") String uuid,HttpSession session

    ) {
        if (logger.isDebugEnabled()) {
            logger.debug("turn to add on the  NewUserInfoController.unlock, uuid=[{}]", new Object[]{uuid});
        }
        /*boolean result = redisTemplate.hasKey(CacheKey.getCacheUserIsfreezed(uuid));
        if (result == true) {*/
            String companyId=ConstantsCMP.getSessionCompanyId(request);
            redisClient.get(CacheKey.getCacheOrganitionTreeList(companyId));
            redisTemplate.delete(CacheKey.getUserLoginFailedInfoCacheKey(uuid));
            redisTemplate.delete(CacheKey.getCacheUserIsfreezed(uuid));
//        }
        try {
            //管理员日志
            UserInfoDomain user = userService.getUserInfo(uuid);
            String admin=ConstantsCMP.getCipherUuidInfo(request);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),  "解锁用户:"+user.getUserName());
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);

        }catch (Exception e){
            e.printStackTrace();
        }

        ResponseUtils.customSuccessResponse(response, "操作成功！");

    }


    @RequestMapping(value = "/queryList")
    @ResponseBody
    public List<NewUserInfo> queryUserListByGroupId(HttpServletResponse response, NewUserInfo form) {
        logger.debug("Enter NewUserInfoController.queryData");
        return newUserService.getUserListByGroupId(form);
    }


    @RequestMapping(value = "/doAuth")
    @ResponseBody
    public Map<String,Object> doAuth(String accountNumber) {
        Map<String,Object> content=new HashMap<>();
        UserInfoDomain userInfoDomain = userService.getUserByAccountNumber(accountNumber);
        content.put("accountNumber", accountNumber);
        content.put("userName", userInfoDomain.getUserName());
        return successResponse("content",content);
    }


    @RequestMapping(value = "/common")
    @ResponseBody
    public List<UserInfoDomain> getUserList() {
     List<UserInfoDomain> userList=userService.getAllUser();
     return  userList;
    }



    public static List<TreeNodesDomain> getUrlList(int queryType){
        List<TreeNodesDomain> UrlList=new ArrayList<>();
        TreeNodesDomain first=new TreeNodesDomain();
        first.setText("组织结构");
        first.setHref("/cipher/newUser/list");
        UrlList.add(first);
        TreeNodesDomain second=new TreeNodesDomain();
        if(queryType==1){
            second.setText("无部门用户");
        }else if(queryType==2){
            second.setText("锁定用户");
        }else {
            second.setText("最近30天未登录用户");
        }
        second.setHref("/cipher/welcomeuser/newList?queryType=" + queryType);
        UrlList.add(second);
        return UrlList;




    }

    @Autowired
    private CheckUserService checkUserService;

     /*
     *添加用户信息接口
     * (已改，生成自增长主键uuid)
     * (数据隔离调整)
     * */
     @CheckToken
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addUser(UserInfoModel form,HttpServletRequest request,HttpSession session) {
         String companyId=ConstantsCMP.getSessionCompanyId(request);
         Map<String, Object> mapFlag = new HashMap<>();
         Map<String, Object> map = new HashMap<>();
         mapFlag.put("return_code", ConstantsCMP.Code.FAIL);
         //TODO
        //用户数量控制
        /* int num= userService.queryUserNumber(companyId);
         if(num>=48){
             mapFlag.put("msg","账号数量不能多于48位");
             return mapFlag;
         }*/
         redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));
         redisClient.remove(CacheKey.getCacheGroupTreeList(companyId));
        Integer flag = checkUserService.checkUserInfo(companyId, form.getAccountNumber(), form.getMail(), form.getPhoneNumber());

        if(flag.equals(1)){
            mapFlag.put("msg","主账号发生重复！");
            return mapFlag;
        }else if(flag.equals(2)){
            mapFlag.put("msg","邮箱发生重复！");
            return mapFlag;
        }else if(flag.equals(3)){
            mapFlag.put("msg","手机号发生重复！");
            return mapFlag;
        }else {
            UserInfoDomain domain = userService.getUserByAccountNumber(form.getUuid());
            form.setSource("newAdd");
            try {
                if (null != domain) {
                    if (domain.getIsDelete() == 1) {
                        userService.deleteUserInfoByAccountNumber(form.getUuid());
                    }
                    if (form.getUuid().equals(domain.getUuid())) {
                        map.put("return_code", 1);
                        map.put("msg", "账号已存在，请重新添加其他账号");
                        return map;
                    }
                }
                form.setCompanyId(companyId);
                userService.insetIntoUserTable(form);
                if (StringUtils.isNotEmpty(form.getDepartment())) {
                    String[] groupIdList = form.getDepartment().split(",");
                    for (String groupId : groupIdList) {
                        GroupUserMapDomain groupUserMapDomain = new GroupUserMapDomain();
                        groupUserMapDomain.setUserId(form.getUuid());
                        groupUserMapDomain.setGroupId(Integer.valueOf(groupId));
                        userGroupService.insertUserGroup(groupUserMapDomain);
                    }
                }else {
                    GroupUserMapDomain groupUserMapDomain = new GroupUserMapDomain();
                    groupUserMapDomain.setUserId(form.getUuid());
                    groupUserMapDomain.setGroupId(0);
                    userGroupService.insertUserGroup(groupUserMapDomain);
                }

                if (StringUtils.isNotEmpty(form.getSecurityGroup())) {
                    String[] teamIdList = form.getSecurityGroup().split(",");
                    for (String teamId : teamIdList) {
                        TeamUserMapInfo teamUserMapInfo = new TeamUserMapInfo();
                        teamUserMapInfo.setUserId(form.getUuid());
                        teamUserMapInfo.setTeamId(Integer.valueOf(teamId));
                        teamUserMapInfoService.insertSelective(teamUserMapInfo);
                    }
                }
                AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.ACCOUNT_MAINTENANCE.getType(),  "添加用户账号:"+form.getAccountNumber());
                adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
                map.put("uuid", form.getUuid());
                map.put("return_code", ConstantsCMP.Code.SUCCESS);
                map.put("msg", "添加用户成功");
            } catch (Exception e) {
                map.put("return_code", ConstantsCMP.Code.FAIL);
                map.put("msg", "服务器错误");
                e.printStackTrace();
            }
        }
        return map;
    }
}
