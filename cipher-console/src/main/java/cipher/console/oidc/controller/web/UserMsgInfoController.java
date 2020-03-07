package cipher.console.oidc.controller.web;


import cipher.console.oidc.common.*;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.mapper.SubAccountMapper;
import cipher.console.oidc.mapper.TeamInfoMapper;
import cipher.console.oidc.mapper.UserGroupMapper;
import cipher.console.oidc.model.GroupApplicationModel;
import cipher.console.oidc.model.SubAccountAuthModel;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.*;
import cipher.console.oidc.service.impl.ObjectServiceFactory;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.IpUtil;
import cipher.console.oidc.util.aes.AES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping(value = "/cipher/userMsg")
@EnableAutoConfiguration
public class UserMsgInfoController {


    private static final Logger logger = LoggerFactory.getLogger(UserMsgInfoController.class.getSimpleName());


    @Autowired
    private UserGroupMapper userGroupMapper;

    @Autowired
    private ApplicationGroupService applicationGroupService;

    @Autowired
    private RoleUserMapService roleUserMapService;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    @Autowired
    private TeamUserMapInfoService teamUserMapInfoService;

    @Autowired
    private GroupService groupService;
    @Autowired
    private SubAccountMapService subAccountMapService;

    @Autowired
    private SubAccountService subAccountService;

    @Autowired
    private SubAccountMapper subAccountMapper;

    @Autowired
    private UserService userService;
    @Autowired
    private TeamInfoMapper teamInfoMapper;

    @Autowired
    private RedisClient<String,Object> redisClient;



    /**
     * modifyUser tiankang
     *
     * @param accountNumber
     * @param groupId
     * @param teamId
     * @param queryType
     * @return 获取用户信息
     * (数据隔离修改)
     */
    @CheckToken
    @RequestMapping(value = "/modifyUser")
    @ResponseBody
    public Map<String, Object> modifyUser(@RequestParam(value = "accountNumber", required = false, defaultValue = "") String accountNumber,
                                          @RequestParam(value = "groupId", required = false) Integer groupId,
                                          @RequestParam(value = "teamId", required = false) Integer teamId,
                                          @RequestParam(value = "queryType", required = false) Integer queryType,
                                          @RequestParam(value = "uuid") String uuid, HttpSession session,
                                          HttpServletRequest request) {

        accountNumber = accountNumber.replace("/", "");
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        redisClient.remove(CacheKey.getCacheOrganitionTreeList(companyId));

        if (logger.isDebugEnabled()) {
            logger.debug("turn to UserMsgInfoController on the modifyUser,accountNumber=[{}],groupId=[{}],teamId=[{}] queryType=[{}],companyId=[{}]", new Object[]{accountNumber, groupId, teamId, queryType, companyId});
        }
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> msg = new HashMap<>();
        try {
            //获取用户个人信息 并验证来源是否为空 是就将来源设置为新添加的账号
            UserInfoDomain user = userService.getUserByAccountNumber(uuid);
            if (user.getSource() != null && StringUtils.isEmpty(user.getSource())) {
                user.setSource("newAdd");
            }
            //获取用户所在的部门
            List<GroupInfoDomain> groupList = groupService.getGroupNamesById(uuid);

            //获取部门转换后的数据
            List<IdentityGroupMapDomain> identityGroupMapDomains = ObjectServiceFactory.getIdentityGroupMapDomain(groupList);

            //构造用户部门结构树
            List<TreeNodesDomain> groupTrees = groupService.getNodeTree(identityGroupMapDomains, companyId);

            //获取安全组对象
            TeamUserMapInfo teamUserMapInfo = ObjectServiceFactory.getTeamUserMapInfo(uuid);

            //获取用户所在安全组列表
            List<TeamInfo> teamInfoList = teamUserMapInfoService.selectTeamUserInfoList(teamUserMapInfo);

            //获取所有安全组
            List<TeamInfo> allTeams = teamInfoMapper.getTeamList(companyId);

            //设置所有安全组组中该应用授权的应用
            List<TeamApplicationChecked> teamApplicationMaps = ObjectServiceFactory.getCheckedTeams(allTeams, teamInfoList);

            //获取钉钉绑定
            int count = userService.getDingDingCount(uuid);

            //统一设置路劲信息
            groupList = groupService.getPath(groupTrees, groupList);


            //是否绑定大白认证
            int count3 = userService.getDabbyCount(uuid);
            //是否绑定赛赋认证
            int count2 = userService.getSaifubinding(uuid);

            //获取AD绑定的账号
            String account = userService.getAdBindAccount(uuid);

            String source = userService.getAdBinding(uuid);

            //获取微信绑定的接口
            boolean wxAccount=userService.getWxAccount(uuid)!=0? true:false;



            //向map中添加信息
            msg.put("user", user);
            msg.put("groupId", groupId);
            msg.put("queryType", queryType);
            msg.put("groupList", groupList);
            msg.put("teamList", teamInfoList);
            msg.put("groupTrees", groupTrees);
            msg.put("bindingDingDing", count > 0);
            msg.put("saiFuBinding", count2 > 0);
            msg.put("dabbyBinding", count3 > 0);
            msg.put("wxBinding", wxAccount);

            msg.put("adbinding", account);
            msg.put("teamApplicationMaps", teamApplicationMaps);
            map.put("msg", msg);
            map.put("code", 0);
        } catch (Exception e) {
            map.put("code", 1);
            map.put("msg", "内部服务器故障");
            e.printStackTrace();
        }
        return map;
    }

    public List<TeamApplicationChecked> getCheckedTeams(List<TeamInfo> allTeams, List<TeamInfo> checkedTeams) {
        List<TeamApplicationChecked> list = new ArrayList<>();
        for (TeamInfo teamInfo : allTeams) {    //循环判断该应用授权的安全组是否在其中，如果是就把state.checked=true
            TeamApplicationChecked teamApplicationMap = new TeamApplicationChecked(teamInfo);
            for (TeamInfo teaminfo1 : checkedTeams) {
                if (teamInfo.getId() == teaminfo1.getId()) {
                    teamApplicationMap.setState();
                }
            }
            list.add(teamApplicationMap);


        }
        return list;

    }


    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addNewUser(@RequestParam(value = "accountNumber", required = false, defaultValue = "") String accountNumber,
                                          @RequestParam(value = "roleId", required = false) Integer roleId) {
        Map<String, Object> map = new HashMap<>();
        try {
            if (StringUtils.isEmpty(accountNumber) || StringUtils.isEmpty(roleId)) {
                map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
                map.put("msg", "参数错误");
                return map;
            } else {
                RoleUserMapInfo form = new RoleUserMapInfo();
                form.setUserId(accountNumber);
                form.setRoleId(roleId);
                roleUserMapService.deleteRoleUser(form);
                map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
                map.put("msg", "删除成功");
            }

        } catch (Exception e) {
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            map.put("msg", "服务器错误");
            e.printStackTrace();
        }
        return map;
    }


    @RequestMapping(value = "/addTeam", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addApp(TeamUserMapInfo form, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        try {
            TeamUserMapInfo teamUserMapInfo = new TeamUserMapInfo();
            teamUserMapInfo.setUserId(form.getUserId());
            teamUserMapInfoService.deleteUserMap(teamUserMapInfo);
            String teamList = form.getTeamIds();
            String groupList = form.getGroupIds();
            if (org.apache.commons.lang.StringUtils.isNotEmpty(teamList)) {
                String[] teamIds = teamList.split(",");
                for (int i = 0; i < teamIds.length; i++) {
                    TeamUserMapInfo domain = new TeamUserMapInfo();
                    domain.setTeamId(Integer.valueOf(teamIds[i]));
                    domain.setUserId(form.getUserId());
                    teamUserMapInfoService.insertSelective(domain);
                }
            }

            if (org.apache.commons.lang.StringUtils.isNotEmpty(groupList)) {
                String[] groupIds = groupList.split(",");
                if (groupIds.length > 1) {
                    boolean flag = Arrays.asList(groupIds).contains("0");
                    if (flag == true) {
                        map.put("return_msg", "部门选择不能同时包含无部门和有部门，请重新选择");
                        map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
                        return map;
                    }
                }
                userGroupMapper.deleteUserGroupMap(form.getUserId());
                for (int i = 0; i < groupIds.length; i++) {
                    GroupUserMapDomain domain = new GroupUserMapDomain();
                    domain.setGroupId(Integer.valueOf(groupIds[i]));
                    domain.setAccountNumber(form.getUserId());
                    userGroupMapper.insertUserGroupMap(domain);
                }
            }
            map.put("return_msg", "操作成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);

        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        return map;
    }


    /*
     * 根据用户获取重账号列表
     * （数据隔离修改）
     * */
    //@CheckToken
    @RequestMapping(value = "/getSublist", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getSublist(@RequestParam(value = "accountNumber", required = false, defaultValue = "") String accountNumber,
                                          String groupId, DataGridModel page, HttpServletRequest request,
                                          @RequestParam(value = "uuid", required = false) String uuid
    ) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        Map<String, Object> map = new HashMap<>();
        GroupInfoDomain groupInfoDomain = null;
        if (StringUtils.isEmpty(groupId)) {
            groupInfoDomain = groupService.queryGroupByAccountName(uuid);
        } else {
            groupInfoDomain = groupService.getGroupById(Integer.valueOf(groupId));
        }
        List<GroupApplicationModel> applications = new ArrayList<>();
        if (null != groupInfoDomain && !StringUtils.isEmpty(groupInfoDomain.getGroupId())) {
            applications = applicationGroupService.selectNewApplicationList(uuid, String.valueOf(groupInfoDomain.getGroupId()), companyId);
        } else if (groupId.equals("0")) {
            applications = applicationGroupService.selectNewApplicationList(uuid, groupId, companyId);
        }

        int pages = applications.size() / page.getRows();
        if (pages * page.getRows() != applications.size()) {
            pages++;
        }
        List<GroupApplicationModel> newList = null;
        if (applications.size() > 0) {
            if (page.getPage() == pages) {
                newList = applications.subList((page.getPage() - 1) * page.getRows(), applications.size());
                map.put("rows", newList);
            } else {
                newList = applications.subList((page.getPage() - 1) * page.getRows(), (page.getPage()) * page.getRows());
                map.put("rows", newList);
            }

        }
        map.put("total", pages);
        map.put("records", applications.size());
        return map;
    }


    @CheckToken
    @RequestMapping(value = "/doSaveSub", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> doSaveSub(SubAccountInfoDomain form,HttpSession session) throws Exception {
        Map<String, Object> map = new HashMap<>();
        SubAccountDomain subAccountDomain = null;
        try {
            if (null != form && !StringUtils.isEmpty(form.getSubAccount()) && !StringUtils.isEmpty(form.getAppClientId())) {
                subAccountDomain = subAccountService.getTheSubAccount(form.getSubAccount(), form.getAppClientId());
                if (null == subAccountDomain) {
                    String password = AES.encryptToBase64(form.getPassword(), ConstantsCMP.AES_KEY);
                    form.setPassword(password);
                    int subId = subAccountService.insertSubAccountInfo(form);
                    SubAccountDomain subDomain = subAccountMapper.querySubAccountInfo(form.getAccountNumber(), form.getAppClientId());
                    if (null != subDomain) {
                        SubAccountAuthModel subAccountAuthModel = new SubAccountAuthModel();
                        subAccountAuthModel.setSubId(subDomain.getId());
                        subAccountAuthModel.setAccountNumber(form.getAccountNumber());
                        subAccountMapper.deleteSubAccountMap(subAccountAuthModel);
                    }
                    SubAccountMapDomain subAccountMapDomain = new SubAccountMapDomain();
                    subAccountMapDomain.setSubId(form.getId());
                    subAccountMapDomain.setAccountNumber(form.getAccountNumber());
                    SubAccountMapDomain domain = subAccountMapService.querySubMap(subAccountMapDomain);
                    if (null == domain) {
                        subAccountMapService.insertSubAccountMap(subAccountMapDomain);
                    }
                } else {
                    SubAccountMapDomain newDomain = new SubAccountMapDomain();
                    newDomain.setAccountNumber(form.getAccountNumber());
                    newDomain.setSubId(subAccountDomain.getId());
                    SubAccountDomain subDomain = subAccountMapper.querySubAccountInfo(form.getAccountNumber(), form.getAppClientId());
                    if (null != subDomain) {
                        SubAccountAuthModel subAccountAuthModel = new SubAccountAuthModel();
                        subAccountAuthModel.setSubId(subDomain.getId());
                        subAccountAuthModel.setAccountNumber(form.getAccountNumber());
                        subAccountMapper.deleteSubAccountMap(subAccountAuthModel);
                    }
                    SubAccountMapDomain domain = subAccountMapService.querySubMap(newDomain);
                    if (null == domain) {
                        subAccountMapService.insertSubAccountMap(newDomain);
                    }
                }
                map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
                map.put("return_msg", "添加从账号成功");
            } else {
                map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
                map.put("return_msg", "参数错误");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SUBACCOUNT_MAINTENANCE.getType(),  "更新子账号信息");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return map;
    }


    public static List<TreeNodesDomain> getUrlList(int queryType, String groupId) {

        List<TreeNodesDomain> UrlList = new ArrayList<>();
        TreeNodesDomain first = new TreeNodesDomain();
        if (groupId.equals("null")) {
            first.setText("组织结构");
            first.setHref("/cipher/newUser/list");
        } else {
            first.setText("首页");
            first.setHref("/cipher/welcome/list");
        }
        UrlList.add(first);
        TreeNodesDomain second = new TreeNodesDomain();
        if (queryType == 1) {
            second.setText("无部门用户");
        } else if (queryType == 2) {
            second.setText("锁定用户");
        } else {
            second.setText("最近30天未登录用户");
        }
        second.setHref("/cipher/welcomeuser/newList?queryType=" + queryType);
        UrlList.add(second);
        TreeNodesDomain third = new TreeNodesDomain();
        third.setText("查看详情");
        third.setHref("");
        UrlList.add(third);
        return UrlList;


    }


    public static void main(String[] args) {
        List<TreeNodesDomain> list = getUrlList(1, "0");
        System.out.println(list);
    }
}
