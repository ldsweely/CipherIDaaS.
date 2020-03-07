package cipher.console.oidc.controller.web;

import cipher.console.oidc.admapper.LdapUserAttributeMapper;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdAuthEnum;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.model.AdMap2LocalConfigModel;
import cipher.console.oidc.model.AdOuModel;
import cipher.console.oidc.service.*;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.IpUtil;
import cipher.console.oidc.util.NumberUtil;
import cipher.console.oidc.util.ResponseUtils;
import cipher.console.oidc.util.aes.AES;
import cipher.console.oidc.util.aes.AesKey;
import cipher.console.oidc.util.aes.RandomUtil;
import cipher.console.oidc.util.rsa.RSATool;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.unboundid.ldap.sdk.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * @Author: zt
 * @Date: 2018/6/7 16:38
 */
@Controller
@RequestMapping(value = "/cipher/ldap")
@EnableAutoConfiguration
public class LdapReadController {


    @Autowired
    private AdInfoService adInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private SessionService sessionService;


    private static final Logger LOGGER = LoggerFactory.getLogger(LdapReadController.class);

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String page(HttpServletResponse response) {
        return "ldap/list";
    }

    @RequestMapping(value = "/getOuTree")
    @ResponseBody
    public List<AdOuModel> queryAdOu(
            HttpSession httpSession,
            AdInfoDomain adInfoDomain,
            @RequestParam(value = "queryStr", required = false) String queryName) {
        try {
            String companyUUid = sessionService.getCompanyUUid(httpSession);
            companyUUid="123456";
            adInfoDomain.setCompanyId(companyUUid);
            return adInfoService.queryAdOuTree(adInfoDomain, queryName);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (LDAPException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 添加Ad域连接信息
     *
     * @param
     * @param
     * @param adInfoDomain
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> add(AdInfoDomain adInfoDomain,AdAutoSyncInfo adAutoSyncInfo,HttpServletRequest request,HttpSession session) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        adInfoDomain.setCompanyId(companyId);

        Map<String, Object> map = new HashMap<>();

        List<AdMap2LocalConfigModel> list = adInfoDomain.getAdMap2LocalDomainList();

        //在这里添加，需要额外导入的字段
        list.add(new AdMap2LocalConfigModel("userAccountControl", "userAccountControl"));
//            list.add(new AdMap2LocalConfigModel("whenCreated","whenCreated"));
        //这个字段whenChange一定得加
        list.add(new AdMap2LocalConfigModel("whenChanged", "whenChanged"));
//            list.add(new AdMap2LocalConfigModel("objectSid","objectSid"));
        list.add(new AdMap2LocalConfigModel("objectGUID", "objectGUID"));
        list.add(new AdMap2LocalConfigModel("distinguishedName", "dn"));

        String kvConfig = new Gson().toJson(list);
        adInfoDomain.setKvConfig(kvConfig);


        int count = adInfoService.queryAdCountByIp(adInfoDomain);
        if (count > 0) {
            map.put("code", 1);
            map.put("msg", "该AD已经添加过");
            return map;
        }
        //添加自动同步配置信息
        if(adAutoSyncInfo!=null){
            adInfoDomain.setAutoConfig(new Gson().toJson(adAutoSyncInfo));
        }
        try {
            adInfoService.insertAdInfo(adInfoDomain);
            map.put("code", 0);
            map.put("msg", "/cipher/ldap/list");
        } catch (Exception e) {
            map.put("code", 1);
            map.put("msg", "服务器内部错误");
            e.printStackTrace();

        }
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"添加ad配置信息");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return map;
    }

    /**
     * create by 田扛
     * create time 2019年3月21日11:15:10
     * 根据id删除单个ad域
     */
    @RequestMapping(value = "deleteAd")
    @ResponseBody
    public Map<String, Object> deleteAd(AdInfoDomain adInfoDomain,HttpSession session) {
        AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"删除ad配置信息");
        adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        return adInfoService.deleteAd(adInfoDomain);

    }

    /**
     * 查询AD配置信息
     * （数据隔离修改）
     *
     * @param response
     * @return
     */
    @RequestMapping(value = "/list", params = "json", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> queryDataInfo(HttpServletResponse response, HttpServletRequest request,
                                             AdInfoDomain form,
                                             DataGridModel pageModel) {
        LOGGER.debug("enter LdapController.queryDataInfo");
        try {
            String companyId = ConstantsCMP.getSessionCompanyId(request);
            form.setCompanyId(companyId);
            return adInfoService.list(form, pageModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @RequestMapping(value = "/queryOne", method = RequestMethod.POST)
    @ResponseBody
    public AdInfoDomain queryDataById(HttpServletRequest request, HttpServletResponse response, @RequestParam(value = "id") Integer id) {
        LOGGER.info("进入根据id查询配置信息:{}", id);
        AdInfoDomain adInfoDomain = adInfoService.queryAdInfoById(id);
        String adAutoSyncInfo = adInfoService.queryAdSyncInfoById(id);
        if (adInfoDomain == null) {
            return null;
        }

        String kvConfigJson = adInfoDomain.getKvConfig();
        List<AdMap2LocalConfigModel> adMap2LocalConfigModelList = new Gson().fromJson(kvConfigJson, new TypeToken<List<AdMap2LocalConfigModel>>() {
        }.getType());

        adInfoDomain.setAdMap2LocalDomainList(adMap2LocalConfigModelList);
        adInfoDomain.setAdAutoSyncInfo(new Gson().fromJson(adAutoSyncInfo, AdAutoSyncInfo.class));
        return adInfoDomain;
    }


    /**
     * 插入或更改ad域的配置
     * (数据隔离修改)
     *
     * @param response
     * @param form
     */

    /**
     * 增加ad自动同步配置
     * create by cozi
     * @date 2019-08-12
     * @param adAutoSyncInfo 自动同步配置信息
     */
    @RequestMapping(value = "/update")
    public void insertAdInfo(HttpServletResponse response, HttpServletRequest request, AdInfoDomain form,AdAutoSyncInfo adAutoSyncInfo,HttpSession session) {
        LOGGER.debug("enter LdapController.insertAdInfo");
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        form.setCompanyId(companyId);
        try {
            adInfoService.updateAdInfo(form);
            adInfoService.updateAdAutSync(form.getId(),companyId,new Gson().toJson(adAutoSyncInfo));
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),"更新ad域的配置");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
            ResponseUtils.customSuccessResponse(response, "更新AD配置信息成功");
        } catch (Exception e) {
            LOGGER.error("enter LdapController.insertAdInfo; Error:" + e.getCause());
            ResponseUtils.customFailueResponse(response, "更新配置失败，请稍后重试");
            e.printStackTrace();
        }
    }


    @RequestMapping(value = "/testConnect", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> testConnect(HttpServletResponse response, AdInfoDomain form) {
        LOGGER.debug("enter LdapController.testConnect");

        AdAuthEnum adAuthEnum = connect(form.getIp(), String.valueOf(form.getPort()), form.getUserName(), form.getPassword());

        Map<String, Object> map = new HashMap<>();
        map.put("return_code", adAuthEnum.getCode());
        map.put("return_msg", adAuthEnum.getDesc());
        return map;
    }


    @RequestMapping(value = "/import", method = RequestMethod.POST)
    @ResponseBody
    public void importAdUser(HttpServletResponse response, AdInfoDomain form,HttpSession session) {
        LOGGER.debug("enter LdapController.import");
        try {
            List<AdUserInfoDomain> list = importUser(form);
            if (CollectionUtils.isEmpty(list)) {
                ResponseUtils.customSuccessResponse(response, "该域中未获取到数据");
                return;
            }
            /***********************同步Ad域中用户到用户体系中（cipher_user_info）**********************/
            List<UserInfoDomain> repeatAccountList = userService.queryRepeatNameUserList(list);
            if (CollectionUtils.isEmpty(repeatAccountList)) {
                userService.insertAdUserList(list);
                //同步用户及其组信息
                for (AdUserInfoDomain adUserInfoDomain : list) {
                    synacAdUserAndGroup(adUserInfoDomain);
                }
                ResponseUtils.customSuccessResponse(response, "同步成功" + list.size() + "条数据");
            } else {
                List<AdUserInfoDomain> tmpList = new ArrayList<>(list);
                List<AdUserInfoDomain> uniqueAdUser = getUniqueUserAccountList(tmpList, repeatAccountList);

                if (!CollectionUtils.isEmpty(uniqueAdUser)) {
                    userService.insertAdUserList(uniqueAdUser);
                    for (AdUserInfoDomain userInfoDomain : uniqueAdUser) {
                        synacAdUserAndGroup(userInfoDomain);
                    }

                    AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.ACCOUNT_MAINTENANCE.getType(),"同步AD用户");
                    adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
                    ResponseUtils.customSuccessResponse(response, "同步成功,重复" + repeatAccountList.size() + "条数据;" + "同步" + (list.size() - repeatAccountList.size()) + "个新用户");
                } else {
                    ResponseUtils.customSuccessResponse(response, "所有用户均已存在，没有新增用户");
                }
            }

            /*************************数据单独维护一份到表**************************************/
            //已经存在在数据库中的用户
         /*   List<AdUserInfoDomain> mailRepeatList = adUserInfoService.queryRepeatName(list);
            if (CollectionUtils.isEmpty(mailRepeatList)) {
                adUserInfoService.insertAdUserInfoList(list);
                ResponseUtils.customSuccessResponse(response, "同步成功" + list.size() + "条数据");
                return;
            }
            int importTotal = list.size();
            int repeat = mailRepeatList.size();
            List<AdUserInfoDomain> uniqueList = getUniqueUserAdList(list, mailRepeatList);
            if (CollectionUtils.isEmpty(uniqueList)) {
                ResponseUtils.customSuccessResponse(response, "同步成功,重复" + repeat + "条数据;" + "同步" + (importTotal - repeat) + "个新用户");
                return;
            }
            adUserInfoService.insertAdUserInfoList(uniqueList);
            ResponseUtils.customSuccessResponse(response, "同步成功,重复" + repeat + "条数据;" + "同步" + (importTotal - repeat) + "个新用户");*/
        } catch (NamingException e) {
            LOGGER.debug("enter LdapController.import;error:" + e.getCause());
            ResponseUtils.customFailueResponse(response, "同步用户失败,请检查网络！");
            e.printStackTrace();
        }

    }

    /**
     * 同步Ad域的用户和组信息到数据库
     */
    private void synacAdUserAndGroup(AdUserInfoDomain adUserInfoDomain) {
        //TODO
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        List<GroupInfoDomain> allGroup = groupService.getAllGroup(companyId);
        String dept = adUserInfoDomain.getUniqueName();
        String[] depts = dept.split(",");
        Integer groupId = null;
        //遍历用户携带的组信息
        for (int i = depts.length - 1; i >= 1; i--) {
            String[] deptName = depts[i].split("=");
            //组名
            String deptNameValue = deptName[1];
            GroupInfoDomain groupInfoDomain = checkContainGroup(allGroup, deptNameValue);
            //该组不存在，创建该组,并且设置该组的上级组织

            if (groupInfoDomain != null) {
                groupId = groupInfoDomain.getGroupId();
            }

            if (groupInfoDomain == null) {
                GroupInfoDomain newGroup = new GroupInfoDomain();
                if (i != depts.length - 1) {
                    String parentDpt = depts[i + 1];
                    String[] deptStr = parentDpt.split("=");
                    GroupInfoDomain parentGroup = checkContainGroup(allGroup, deptStr[1]);
                    newGroup.setParentGroupId(parentGroup.getGroupId());
                }
                newGroup.setOu(deptNameValue);
                newGroup.setGroupName(deptNameValue);
                newGroup.setGroupDescription("从AD导过来的组");
                newGroup.setModifyTime(new Date());
                newGroup.setCreateTime(new Date());
                //插入新的组，
                groupService.insertGroup(newGroup);
                groupId = newGroup.getGroupId();
                allGroup.clear();
                allGroup = groupService.getAllGroup(companyId);
            }
        }
        //绑定用户和组的关系
        GroupUserMapDomain groupUserMapDomain = new GroupUserMapDomain();
        groupUserMapDomain.setAccountNumber(adUserInfoDomain.getMail());
        groupUserMapDomain.setGroupId(groupId);
        userGroupService.insertUserGroup(groupUserMapDomain);
    }

    /**
     * 判断数据库中是否包含某个组
     *
     * @param allGroup  所有的组
     * @param groupName 组名
     * @return 若找到组则返回改组，否则返回空
     */
    private GroupInfoDomain checkContainGroup(List<GroupInfoDomain> allGroup, String groupName) {
        if (CollectionUtils.isEmpty(allGroup)) {
            return null;
        }
        for (GroupInfoDomain groupInfoDomain : allGroup) {
            if (groupName.equals(groupInfoDomain.getOu())) {
                return groupInfoDomain;
            }
        }
        return null;
    }

    /**
     * 从导入的用户列表中取出已经重复的用户
     *
     * @param importList
     * @param repeatList
     * @return
     */
    private List<AdUserInfoDomain> getUniqueUserAdList(List<AdUserInfoDomain> importList, List<AdUserInfoDomain> repeatList) {
        Iterator<AdUserInfoDomain> iterator = importList.iterator();

        while (iterator.hasNext()) {
            AdUserInfoDomain adUserInfoDomain = iterator.next();
            for (AdUserInfoDomain infoDomain : repeatList) {
                if (adUserInfoDomain.getMail().equals(infoDomain.getMail())) {
                    iterator.remove();
                    break;
                }
            }
        }
        return importList;
    }


    /**
     * 去掉从Ad域中导入的用户列表中与已有用户列表中重复的数据
     *
     * @param importList
     * @param repeatList
     * @return
     */
    private List<AdUserInfoDomain> getUniqueUserAccountList(List<AdUserInfoDomain> importList, List<UserInfoDomain> repeatList) {
        Iterator<AdUserInfoDomain> iterator = importList.iterator();
        while (iterator.hasNext()) {
            AdUserInfoDomain adUserInfoDomain = iterator.next();
            for (UserInfoDomain infoDomain : repeatList) {
                if (infoDomain.getMail().equals(adUserInfoDomain.getMail())) {
                    iterator.remove();
                    break;
                }
            }
        }
        return importList;
    }


    /**
     * 测试AD能否正常连接
     *
     * @param host
     * @param port
     * @param username
     * @param password
     */
    private AdAuthEnum connect(String host, String port, String username, String password) {
        DirContext ctx = null;
        Hashtable<String, String> hashEnv = new Hashtable<String, String>();
//        HashEnv.put(Context.SECURITY_AUTHENTICATION, "simple"); // LDAP访问安全级别(none,simple,strong)
        hashEnv.put(Context.SECURITY_PRINCIPAL, username); //AD的用户名
        hashEnv.put(Context.SECURITY_CREDENTIALS, password); //AD的密码
        hashEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory"); // LDAP工厂类
        hashEnv.put("com.sun.jndi.ldap.connect.timeout", "3000");//连接超时设置为3秒
        hashEnv.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port);// 默认端口389
        try {
//            ctx = new InitialDirContext(HashEnv);// 初始化上下文
            ctx = new InitialLdapContext(hashEnv, null);
            System.out.println("身份验证成功!");
            return AdAuthEnum.AUTH_SUCCESS;

        } catch (AuthenticationException e) {
            e.printStackTrace();
            System.out.println("身份验证失败!");
            return AdAuthEnum.AUTH_FAIL;

        } catch (javax.naming.CommunicationException e) {
            System.out.println("AD域连接失败!");
            e.printStackTrace();
            return AdAuthEnum.CONNECT_FAIL;
        } catch (Exception e) {
            System.out.println("身份验证未知异常!");
            e.printStackTrace();
            return AdAuthEnum.SYS_ERROR;
        } finally {
            if (null != ctx) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 导入用户
     *
     * @param form
     * @return
     * @throws NamingException
     */
    @SuppressWarnings("unchecked")
    private List<AdUserInfoDomain> importUser(AdInfoDomain form) throws NamingException {
        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("objectClass", "person"));
        //  查询所有内部人员
        return ldapTemplate.search(form.getUserDn(), filter.encode(), new LdapUserAttributeMapper());
    }


    /*
    * 根据连接方式生成agent的secret
    * */

    public  static String getAgentSecret(){
        String randomCharData= NumberUtil.createRandomCharData(25);
       // String secret= RSATool.encryptWithPubKey(randomCharData, AesKey.AES_KEY);
        return  randomCharData;
    }

    public static void main(String arg[]){
       String secret=getAgentSecret();
       System.out.println(secret);

    }



}
