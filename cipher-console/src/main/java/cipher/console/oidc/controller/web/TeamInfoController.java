package cipher.console.oidc.controller.web;


import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.common.ReturnUtils;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.enums.ConditionEnum;
import cipher.console.oidc.enums.TeamRuleEnum;
import cipher.console.oidc.mapper.GroupMapper;
import cipher.console.oidc.model.UserInfoModel;
import cipher.console.oidc.service.*;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.IpUtil;
import net.sf.json.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import static cipher.console.oidc.common.ReturnUtils.successResponse;

@Controller
@RequestMapping(value = "/cipher/team")
@EnableAutoConfiguration
public class TeamInfoController {

    @Autowired
    private  AppService appService;
    @Autowired
    private TeamInfoService teamInfoService;

    @Autowired
    private TeamUserMapInfoService teamUserMapInfoService;

    @Autowired
    private TeamApplicationMapService teamApplicationMapService;

    @Autowired
    private OrganitionTreeSerive organitionTreeSerive;

    @Autowired
    private NewUserService newUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;


    private static final Logger logger = LoggerFactory.getLogger(TeamInfoController.class);


    /*
    * 获取安全组列表
    * （数据隔离改变）
    * */
    @CheckToken
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> list(TeamInfo form, DataGridModel pageModel,HttpServletRequest request) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        form.setCompanyId(companyId);
        return teamInfoService.getTeamInfoPageList(form, pageModel);
    }

    /**
     * create by 田扛
     * create time 2019年3月21日10:21:05
     * 穿梭狂授权保存接口
     *
     */
    @RequestMapping(value = "/saveTeamApplicationMap")
    @ResponseBody
    public Map<String,Object> saveTeamApplicationMap(TeamInfo teamInfo,HttpSession session){
        try {
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.AUTHORIZED_MAINTENANC.getType(), "安全组授权成功");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
        }catch (Exception e){
            e.printStackTrace();
        }
        return teamInfoService.saveTeamApplicationMap(teamInfo);
    }

    /**
     * create by 田扛
     * create time 2019年3月20日20:09:56
     *
     * 返回该安全组下已授权和未授权的应用结构
     *
     * (数据隔离修改)
     */
    @CheckToken
    @RequestMapping(value = "teamAuthApplication")
    @ResponseBody
    public Map<String,Object> getTeamAuthApplication(TeamInfo teamInfo,HttpServletRequest request){
       String companyId=ConstantsCMP.getSessionCompanyId(request);
       teamInfo.setCompanyId(companyId);
       return teamInfoService.getTeamAuthApplication(teamInfo);
    }

    /**
     * create by 田扛
     * create time 2019年3月20日19:46:18
     * 取消某个安全组下的应用的授权
     * @param form
     * @return
     */

    @RequestMapping(value = "/cancelAuthration")
    @ResponseBody
    public Map<String , Object>  cancelAuthration(TeamInfo form,HttpSession session){
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.TEAM_UPDATE.getType(),  "取消安全组的应用的授权");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return teamInfoService.cancelAuthration(form);
    }

    /**
     * create by 田扛
     * create time 2019年3月20日19:45:16
     * 获取某个安全组下的应用
     *
     * @param teamInfo
     * @param dataGridModel
     * @return
     */
    @RequestMapping(value = "/getTeamApplications")
    @ResponseBody
    public Map<String,Object> getTeamApplications(TeamInfo teamInfo,DataGridModel dataGridModel){

        return teamInfoService.getTeamApplications(teamInfo,dataGridModel);
    }


    /*
    * 修改安全组信息
    * （数据隔离修改）
    *
    * */
    @CheckToken
    @RequestMapping(value = "/addorUpdate", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addorUpdate(TeamInfo form, HttpServletRequest request, HttpSession session) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        Map<String, Object> map = new HashMap<>();
        try {
            form.setCompanyId(companyId);
            TeamInfo teamInfo = teamInfoService.selectTeamInfo(form);
            if (null == teamInfo) {
                if (null == form.getId()) {
                    teamInfoService.insertSelective(form);
                    map.put("return_msg", "添加成功");
                    map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
                } else {
                    teamInfoService.updateByPrimaryKeySelective(form);
                    map.put("return_msg", "修改成功");
                    map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
                }
            } else {
                map.put("return_msg", "安全组名已存在");
                map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            }
        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.TEAM_UPDATE.getType(),  "更新安全组信息");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return map;
    }


    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> delete(Integer id,HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        TeamUserMapInfo teamUserMapInfo = new TeamUserMapInfo();
        teamUserMapInfo.setTeamId(id);
        List<TeamUserMapInfo> list = teamUserMapInfoService.selectTeamUserMap(teamUserMapInfo);
        if (null != list && list.size() > 0) {
            map.put("return_msg", "安全组下有成员，无法删除");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            return map;
        }
        TeamInfo teamInfo=teamInfoService.selectByPrimaryKey(id);
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.TEAM_UPDATE.getType(),  "删除安全组:"+teamInfo.getTeamName());
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        try {
            teamInfoService.deleteInfo(id);
            map.put("return_msg", "删除成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        return map;
    }

  /*
  * 获取安全组成员列表
  * */
    @RequestMapping(value = "/userListPre")
    public Map<String, Object> userList(String id) {
        Map<String, Object> content = new HashMap<>();
        TeamInfo teamInfo = teamInfoService.selectByPrimaryKey(Integer.valueOf(id));
        content.put("teamName", teamInfo.getTeamName());
        content.put("id", id);
        return successResponse("content", content);
    }



    /*
    * 获取安全组成员列表
    * （数据隔离修改）
    *
    * */
    @CheckToken
    @RequestMapping(value = "/userList", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> userList(UserInfoDomain form, DataGridModel pageModel) {
        return teamUserMapInfoService.getTeamUserInfoMapPageList(form, pageModel);
    }



    /*
    *
    * 安全组移除成员
    * （数据隔离修改）
    * */
    @CheckToken
    @RequestMapping(value = "/deleteUserMap", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> deleteUserMap(Integer teamId,HttpSession session,
    @RequestParam(value = "uuid") String uuid, @RequestParam(value = "accountNumber",required = false) String accountNumber
    ) {
        Map<String, Object> map = new HashMap<>();
        try {
            TeamUserMapInfo teamUserMapInfo = new TeamUserMapInfo();
            teamUserMapInfo.setTeamId(teamId);
            teamUserMapInfo.setUserId(uuid);
            teamUserMapInfoService.deleteUserMap(teamUserMapInfo);
            map.put("return_msg", "删除成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        UserInfoDomain userInfoDomain=userService.getUserInfoByCompany(uuid);
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.TEAM_UPDATE.getType(),  "删除安全组成员:"+userInfoDomain.getUserName());
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return map;
    }


    @RequestMapping(value = "/addAppPre")
    @ResponseBody
    public Map<String, Object> appList(String id) {
        TeamInfo teamInfo = teamInfoService.selectByPrimaryKey(Integer.valueOf(id));
        Map<String, Object> content = new HashMap<>();
        TeamApplicationMap record = new TeamApplicationMap();
        record.setTeamId(Integer.valueOf(id));
        List<ApplicationInfo> appList = teamApplicationMapService.selectAppList(record);
        if (null != appList && appList.size() > 0) {
            List<String> appIds = new ArrayList<>();
            for (ApplicationInfo domain1 : appList) {
                appIds.add(String.valueOf(domain1.getId()));
            }
            String str = String.join(",", appIds);
            content.put("appList", str);
        }
        content.put("teamName", teamInfo.getTeamName());
        content.put("id", id);
        return successResponse("content", content);
    }


    @RequestMapping(value = "/addApp", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addApp(TeamApplicationMap form, HttpServletRequest request,HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        try {
            TeamApplicationMap teamApplicationMap = new TeamApplicationMap();
            teamApplicationMap.setTeamId(form.getTeamId());
            teamApplicationMapService.deleteTeamApplicationMap(teamApplicationMap);
            String appList = form.getAppList();
            if (StringUtils.isNotEmpty(appList)) {
                String[] appIds = appList.split(",");
                for (int i = 0; i < appIds.length; i++) {
                    TeamApplicationMap domain = new TeamApplicationMap();
                    domain.setApplicationId(Integer.valueOf(appIds[i]));
                    domain.setTeamId(form.getTeamId());
                    teamApplicationMapService.insertSelective(domain);
                }
            }
            map.put("return_msg", "操作成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);

        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        //管理员日志
        try {

            String admin = ConstantsCMP.getCipherUuidInfo(request);
            String companyId=ConstantsCMP.getSessionCompanyId(request);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.STRATEGY_MAINTENANCE.getType(),  "更新口令管理");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return map;
    }


    @RequestMapping(value = "/addUserPre")
    @ResponseBody
    public Map<String, Object> userPage(@RequestParam(value = "id") Integer id) {
        TeamInfo teamInfo = teamInfoService.selectByPrimaryKey(id);
        Map<String, Object> content = new HashMap<>();
        TeamUserMapInfo record = new TeamUserMapInfo();
        record.setTeamId(id);
        List<UserInfoModel> userList = teamUserMapInfoService.selectUserList(record);
        if (null != userList && userList.size() > 0) {
            List<String> appIds = new ArrayList<>();
            for (UserInfoModel domain1 : userList) {
                appIds.add(String.valueOf(domain1.getAccountNumber()));
            }
            String str = String.join(",", appIds);
            content.put("userIds", str);
        }
        content.put("teamName", teamInfo.getTeamName());
        content.put("id", id);
        return successResponse("content", content);
    }

    /**
     * modify by 田扛
     * modify time 2019年3月11日16:40:44
     * @param id
     * @return
     * (数据隔离调整)
     */
   // @CheckToken
    @RequestMapping(value = "/getUser", method = RequestMethod.POST)
    @ResponseBody
    public Map<String,Object> queryData(String id,HttpServletRequest request) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        Map<String,Object> map=new HashMap<>();
        Map<String,Object> msg=new HashMap<>();
        try{
            TeamUserMapInfo record = new TeamUserMapInfo();
            record.setTeamId(Integer.valueOf(id));
            List<UserInfoModel> userList = teamUserMapInfoService.selectUserList(record);
            List<String> accountNumbers= new  ArrayList();
            for (UserInfoModel userManage:userList) {
                accountNumbers.add(userManage.getUuid());
            }

            List<TreeNodesDomain> trees= appService.getUserTree(accountNumbers,companyId);
            map.put("code",0);
            msg.put("trees",trees);
            map.put("msg",msg);
        }catch (Exception e){
            map.put("code",1);
            map.put("msg","服务器内部错误");
            e.printStackTrace();
            return map;
        }
        return map;
    }

/*
* 安全组下添加用户
* （数据隔离修改）
* */
    @CheckToken
    @RequestMapping(value = "/addUser", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> userUser(TeamUserMapInfo form,HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        try {
            TeamUserMapInfo teamUserMapInfo = new TeamUserMapInfo();
            teamUserMapInfo.setTeamId(form.getTeamId());
            teamUserMapInfoService.deleteUserMap(teamUserMapInfo);
            String userList = form.getUserIds();
            if (StringUtils.isNotEmpty(userList)) {
                String[] userIds = userList.split(",");
                for (int i = 0; i < userIds.length; i++) {
                    TeamUserMapInfo domain = new TeamUserMapInfo();
                    domain.setUserId(userIds[i]);
                    domain.setTeamId(form.getTeamId());
                    TeamUserMapInfo userMapInfo = teamUserMapInfoService.selectTeamUserMapInfo(domain);
                    if (null == userMapInfo) {
                        teamUserMapInfoService.insertSelective(domain);
                    }
                }
            }
            map.put("return_msg", "操作成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);

        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.TEAM_UPDATE.getType(),  "安全组添加或删除用户");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return map;
    }

    /*
    * 根据公司获取安全组所有信息
    * （数据隔离）
    * */
    @CheckToken
    @RequestMapping(value = "/common", params = "json")
    @ResponseBody
    public List<TeamInfo> getTeamList(HttpServletRequest request) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        return teamInfoService.getTeamList(companyId);
    }

    @RequestMapping(value = "/ruleList", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> ruleList(String id) {
        TeamInfo teamInfo = teamInfoService.selectByPrimaryKey(Integer.valueOf(id));
        Map<String, Object> content = new HashMap<>();
        content.put("teamName", teamInfo.getTeamName());
        content.put("id", id);
        return successResponse("content", content);
    }


    @RequestMapping(value = "/ruleContent", method = RequestMethod.POST)
    @ResponseBody
    public List<TypeInfo> getruleContent() {
        TeamRuleEnum[] values = TeamRuleEnum.values();
        List<TypeInfo> list = new ArrayList<>();
        for (TeamRuleEnum teamRuleEnum : values) {
            TypeInfo typeInfo = new TypeInfo();
            typeInfo.setType(teamRuleEnum.getType());
            typeInfo.setDesc(teamRuleEnum.getDesc());
            list.add(typeInfo);
        }
        return list;
    }


    @RequestMapping(value = "/condtionContent", method = RequestMethod.POST)
    @ResponseBody
    public List<TypeInfo> getcontent() {
        ConditionEnum[] values = ConditionEnum.values();
        List<TypeInfo> list = new ArrayList<>();
        for (ConditionEnum conditionEnum : values) {
            TypeInfo typeInfo = new TypeInfo();
            typeInfo.setType(conditionEnum.getType());
            typeInfo.setDesc(conditionEnum.getDesc());
            list.add(typeInfo);
        }
        return list;
    }


    @RequestMapping(value = "/getRuleList", method = RequestMethod.POST)
    @ResponseBody
    public List<RuleInfo> getRuleList(String teamId) {
        List<RuleInfo> ruleInfoList = null;
        TeamInfo teamInfo = teamInfoService.selectByPrimaryKey(Integer.valueOf(teamId));
        if (null != teamInfo && StringUtils.isNotEmpty(teamInfo.getRuleValue())) {
            JSONArray jsonarray = JSONArray.fromObject(teamInfo.getRuleValue());
            System.out.println(jsonarray);
            ruleInfoList = (List) JSONArray.toCollection(jsonarray, RuleInfo.class);
        }
        return ruleInfoList;
    }

   /*
   * 安全组管理
   * 参数规则管理
   * */

    @RequestMapping(value = "/addRule", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addRule(TeamInfo form,HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        try {
            String companyId=ConstantsCMP.getSessionCompanyId(request);
            teamInfoService.updateByPrimaryKeySelective(form);
            List<NewUserInfo> list = new ArrayList<>();
            List<RuleInfo> ruleInfoList = null;
            if (StringUtils.isNotEmpty(form.getRuleValue())) {
                JSONArray jsonarray = JSONArray.fromObject(form.getRuleValue());
                System.out.println(jsonarray);
                ruleInfoList = (List) JSONArray.toCollection(jsonarray, RuleInfo.class);
            }
            int count = 1;
            for (RuleInfo ruleInfo : ruleInfoList) {
                NewUserInfo newUserInfo = new NewUserInfo();
                newUserInfo.setType(ruleInfo.getConditionName());
                newUserInfo.setQueryType(ruleInfo.getCondition());
                newUserInfo.setQueryName(ruleInfo.getParamName());
                newUserInfo.setCompanyId(companyId);
                List<NewUserInfo> newUserInfoList = newUserService.queryUserList(newUserInfo);
                // System.out.println(newUserInfoList);
                if (form.getType() == 1) {
                    //取去重无重复的并集
                    list.removeAll(newUserInfoList);
                    list.addAll(newUserInfoList);
                } else {
                    //取交集
                    if (count == 1) {
                        list.addAll(newUserInfoList);
                        count++;
                    }
                    list.retainAll(newUserInfoList);
                }
            }
            String userName = "";
            String accountNumber = "";
            String uuid="";
            List<String> newList = new ArrayList<>();
            List<String> otherList = new ArrayList<>();
            List<String> uuidList = new ArrayList<>();
            if (null != list && list.size() > 0) {
                for (NewUserInfo newUserInfo : list) {
                    String ss = newUserInfo.getUserName() + "(" + newUserInfo.getAccountNumber() + ")";
                    newList.add(ss);
                    otherList.add(newUserInfo.getAccountNumber());
                    uuidList.add(newUserInfo.getUuid());
                    userName = org.apache.commons.lang3.StringUtils.join(newList, ",");
                    accountNumber = org.apache.commons.lang3.StringUtils.join(otherList, ",");
                    uuid=org.apache.commons.lang3.StringUtils.join(uuidList, ",");
                }
            }
            map.put("userName", userName);
            map.put("userList", list);
            map.put("accountNumber", accountNumber);
            map.put("uuid",uuid);
            map.put("num", list.size());
            map.put("return_msg", "操作成功");
            map.put("return_code", ConstantsCMP.ReturnCode.SUCCESS);
        } catch (Exception e) {
            map.put("return_msg", "服务器错误");
            map.put("return_code", ConstantsCMP.ReturnCode.FAIL);
            e.printStackTrace();
        }
        return map;
    }




    @RequestMapping(value = "/showUser", method = RequestMethod.GET)
    @ResponseBody
    public void show(String userList) {
         ReturnUtils.successResponse("userList", userList);
    }


    @RequestMapping(value = "/showUser", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> showList(DataGridModel page, String userList) throws UnsupportedEncodingException {
        userList = userList.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        userList = userList.replaceAll("\\+", "%2B");
        userList = URLDecoder.decode(userList, "utf-8");
        //JSONArray array = JSONArray.fromObject(userList);
        String listArray[] = userList.split(",");
        List<NewUserInfo> list = new ArrayList<>();
        String groupName = "";
        for (int i = 0; i < listArray.length; i++) {
            NewUserInfo userInfo = newUserService.getNewUserInfo(listArray[i]);
            List<GroupInfoDomain> grouplist = groupMapper.selectGroupNameList(userInfo.getUuid());
            if (null != grouplist && grouplist.size() > 0) {
                List<String> groupNamelist = new ArrayList<>();
                for (GroupInfoDomain groupInfoDomain : grouplist) {
                    groupNamelist.add(groupInfoDomain.getGroupName());
                    groupName = org.apache.commons.lang3.StringUtils.join(groupNamelist, ",");
                    userInfo.setGroupName(groupName);
                }
            } else {
                userInfo.setGroupName(groupName);
            }
            list.add(userInfo);
        }
        Map<String, Object> map = new HashMap<>();
        int pages = list.size() / page.getRows();
        if (pages * page.getRows() != list.size()) {
            pages++;
        }
        List<NewUserInfo> newList = null;
        if (list.size() > 0) {
            if (page.getPage() == pages) {
                newList = list.subList((page.getPage() - 1) * page.getRows(), list.size());
                map.put("rows", newList);
            } else {
                newList = list.subList((page.getPage() - 1) * page.getRows(), (page.getPage()) * page.getRows());
                map.put("rows", newList);
            }
        }
        map.put("return_code", "1");
        map.put("total", pages);
        map.put("records", list.size());
        return map;
    }

     /*
     * 添加安全组规则
     * （数据隔离修改需要前端传参调整）
     * */
     @CheckToken
    @RequestMapping(value = "/addRuleUser", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> addRuleUser(TeamUserMapInfo form) {
        Map<String, Object> map = new HashMap<>();
        try {
            String userList = form.getUserIds();
            if (StringUtils.isNotEmpty(userList)) {
                String[] userIds = userList.split(",");
                for (int i = 0; i < userIds.length; i++) {
                    TeamUserMapInfo domain = new TeamUserMapInfo();
                    domain.setUserId(userIds[i]);
                    domain.setTeamId(form.getTeamId());
                    TeamUserMapInfo teamUserMapInfo = teamUserMapInfoService.selectTeamUserMapInfo(domain);
                    if (null == teamUserMapInfo) {
                        teamUserMapInfoService.insertSelective(domain);
                    }
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
    * 根据用户获取当前用户的安全组信息
    * （数据隔离）
    *
    * */
    @CheckToken
    @RequestMapping(value = "/teamList",method = RequestMethod.POST)
    @ResponseBody
    public List<TeamInfo> getTeamList(@RequestParam(value = "accountNumber",required = false)String accountNumber,
    @RequestParam(value = "uuid",required = false)String uuid,HttpServletRequest request
    ) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        List<TeamInfo> teamInfoList=teamInfoService.getTeamList(companyId);
        TeamUserMapInfo teamUserMapInfo=new TeamUserMapInfo();
          if(StringUtils.isNotEmpty(uuid)){
            teamUserMapInfo.setUserId(uuid);
            List<TeamUserMapInfo> teamInfos=teamUserMapInfoService.selectTeamUserMap(teamUserMapInfo);
            for(TeamInfo teamInfo:teamInfoList){
                for(TeamUserMapInfo team:teamInfos){
                    if(teamInfo.getId()==team.getTeamId()){
                        teamInfo.setState();
                    }
                }
            }
        }
        return teamInfoList;

    }

}
