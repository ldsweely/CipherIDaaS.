package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.common.NewReturnUtils;
import cipher.console.oidc.config.ImgProperties;
import cipher.console.oidc.domain.NullCacheObject;
import cipher.console.oidc.domain.bto.AppInfoBto;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.entity.RSAPublicPrivateKeyEntity;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.model.ApplicationSelectModel;
import cipher.console.oidc.model.ApplicationSubAccountModel;
import cipher.console.oidc.publistener.AppBehaviorPublistener;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.*;
import cipher.console.oidc.service.AdminBehaviorInfoService;
import cipher.console.oidc.service.AppService;
import cipher.console.oidc.service.ImageInfoService;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.IpUtil;
import cipher.console.oidc.util.ResponseUtils;
import cipher.console.oidc.util.RsaSamlUtils;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cipher.console.oidc.common.ReturnUtils.failureResponse;
import static cipher.console.oidc.common.ReturnUtils.successResponse;

/**
 * @Author: zt
 * @Date: 2018/5/28 21:01
 */
@Controller
@RequestMapping(value = "/cipher/app")
@EnableAutoConfiguration
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    @Autowired
    private AppService appService;
    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;
    @Autowired
    private ImgProperties imgProperties;
    @Autowired
    private ImageInfoService imageInfoService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AppBehaviorPublistener appBehaviorPublistener;

    @Autowired
    private RedisClient<String, Object> redisClient;

    @Autowired
    private SystemConfigService systemConfigService;


    /**
     * @param path 图片上传的路径
     * @return
     * @Title: uploadImg 上传的图片流
     * @return: boolean
     */
    private static boolean uploadImg(String path, String imgName, byte[] imgByte) {
        logger.info("enter AppController.imgUpload:parameter+path:" + path + ";imgName+" + imgName);
        // Linux服务器是反斜杠
        path = path.replaceAll("\\\\", "/");
        File filePath = new File(path);
        filePath.setWritable(true, false);
        if (!filePath.exists()) {
            filePath.mkdirs();
        }
        boolean isSuccess = false;
        File file = new File(path + imgName);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(imgByte);
            output.flush();
            isSuccess = true;
        } catch (IOException e) {
            logger.info(" ---->Error enter AppController.imgUpload:parameter+path:" + path + ";imgName+" + imgName + " error:" + e.getCause());
            e.printStackTrace();
            isSuccess = false;
        } finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                logger.info(" ---->Error enter AppController.imgUpload:parameter+path:" + path + ";imgName+" + imgName + " error:" + e.getCause());
                e.printStackTrace();
            }
        }
        return isSuccess;
    }

    @RequestMapping(value = "/add")
    @ResponseBody
    public Map<String, Object> addPage(@RequestParam(value = "applicationType", required = false) String applicationType) {
        List<ImageInfoDomain> list = imageInfoService.getImageList();
        Map<String, Object> map = new HashMap<>();
        map.put("imageList", list);
        return map;
    }

    @RequestMapping(value = "/add", params = "json")
    @ResponseBody
    public Map<String, Object> appAdd(ApplicationInfoDomain form, AssociatedAccountInfo form2, HttpSession session, HttpServletRequest request) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        logger.debug("enter AppController.appAdd;parameters: " + form.toString());
//        //应用数量控制
////        int num = appService.queryAllApplicationNumber(companyId);
////        if (num >= 30) {
////            return NewReturnUtils.failureResponse("应用数量不能超过30个");
////        }

        //saml应用生成应用的公私钥信息
        if (form.getApplicationType() == 10) {
            RSAPublicPrivateKeyEntity rsaPublicPrivateKeyEntity = new RSAPublicPrivateKeyEntity();
            try {
                rsaPublicPrivateKeyEntity = RsaSamlUtils.getPrivateKey(rsaPublicPrivateKeyEntity); //生成公私钥字符串
                //数据添加到privateKey中
                form.setPrivateKey(rsaPublicPrivateKeyEntity.getPrivateKey());
                form.setPublicKey(rsaPublicPrivateKeyEntity.getPublicKey());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                logger.info("enter AppController.ApplicationInfoDomain() error  because produce key", "");
                logger.error(e.getMessage(), e);
                return NewReturnUtils.failureResponse("公私钥生成失败，添加saml应用失败");
            }
        }
        redisClient.remove(CacheKey.getCacheApplicationList(companyId));
        try {
            if (StringUtils.isNotEmpty(form.getApplicationName().trim())) {
                int count = appService.countApplication(form);
                if (count >= 1) {
                    return NewReturnUtils.failureResponse("应用名不能重复");
                }
                Map<String, Object> map = new HashMap<>();
                form.setCompanyId(companyId);

                if (form.getApplicationType() == 7) { //应用类型为cas类型ApplicationType=7
                    form.setAssociatedAccount(new Gson().toJson(form2));
                    form.setCasServerUrl("http://localhost:1111/cipher/cas");
                    form.setCasLogoutUrl("http://localhost:1111/cipher/cas/logout");
                } else {//非cas应用
                    //添加从账号配置默认值
                    AssociatedAccountInfo associatedAccountInfo = new AssociatedAccountInfo();
                    associatedAccountInfo.setAssManual(0);
                    associatedAccountInfo.setAssPrimaryAccount(1);
                    associatedAccountInfo.setAssEmail(1);
                    associatedAccountInfo.setAssEmailPrefix(1);
                    associatedAccountInfo.setAssTelephone(1);
                    associatedAccountInfo.setAssWorkers(1);
                    associatedAccountInfo.setAssPwdManual(0);
                    associatedAccountInfo.setAssPwdPrimaryAccount(1);
                    form.setAssociatedAccount(new Gson().toJson(associatedAccountInfo));
                }
                appService.insertApplication(form);
                ApplicationInfoDomain applicationInfoDomain = appService.getApplication(form);
                map = NewReturnUtils.successResponse("新增应用成功");
                map.put("return_result", applicationInfoDomain.getId());
                AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.APPLICATION_MAINTENANCE.getType(), "添加应用:" + form.getApplicationName());
                adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
                return map;
            }
            try {
                UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
                String userName = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
                AppAuditInfo appAuditInfo = new AppAuditInfo(Integer.valueOf(form.getId()), userName, 2, "新增应用成功", companyId);
                appBehaviorPublistener.publish(appAuditInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return NewReturnUtils.failureResponse("应用名不能为空");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NewReturnUtils.failureResponse("新增应用失败");

    }

    @RequestMapping(value = "sonAccount", params = "json")
    @ResponseBody
    public Map<String, Object> sonAccount(DataGridModel pagemodel, AppSonAccountDomain appSonAccountDomain, HttpServletRequest request) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        return appService.queryAccount(appSonAccountDomain, pagemodel, companyId);
    }

    @RequestMapping(value = "/info")
    @ResponseBody
    public Map<String, Object> info(ApplicationInfoDomain form, @RequestParam(value = "id") String appId, HttpServletResponse response) {
        //FIXME:FIXED-诗昭
        Integer id = Integer.valueOf(appId);
        form.setId(id);
        List<ImageInfoDomain> list = imageInfoService.getImageList();
        ApplicationInfoDomain domain = appService.findApplicationById(form);
        Map<String, Object> map = new HashMap<>();
        map.put("imageList", list);
        map.put("app", domain);
        return map;
    }

    /**
     * 跳转到应用配置界面
     *
     * @param form
     * @param appId
     * @param response
     * @return
     */
    @RequestMapping(value = "/applicationConfig")
    @ResponseBody
    public Map<String, Object> applicationConfig(ApplicationInfoDomain form, @RequestParam(value = "id") String appId, HttpServletResponse response) {
        //FIXME:FIXED-诗昭
        Integer id = Integer.valueOf(appId);
        Map<String, Object> map = new HashMap<>();
        AllRuleInfoDomain ruleInfo = appService.getRuleInfo(appId);
        form.setId(id);
        ApplicationInfoDomain domain = appService.findApplicationById(form);
        if (ruleInfo == null) {
            ruleInfo = new AllRuleInfoDomain();
            map.put("ruleInfo", ruleInfo);
        } else {
            map.put("ruleInfo", ruleInfo);
        }
        /*
        新增从账号配置
        modify by cozi
        date 2019-06-11
         */
        String associatedAccountInfo = appService.associatedAccountById(id);
        AssociatedAccountInfo associatedAccountInfo1 = new Gson().fromJson(associatedAccountInfo, AssociatedAccountInfo.class);
        domain.setAssociatedAccount(associatedAccountInfo1);
        map.put("app", domain);
        return map;
    }

    /**
     * 从账号列表
     */
    @RequestMapping(value = "/subAccount")
    @ResponseBody
    public Map<String, Object> subAccount(ApplicationInfoDomain form, @RequestParam(value = "id") String appId, HttpServletResponse response) {
        //FIXME:FIXED-诗昭
        Integer id = Integer.valueOf(appId);
        form.setId(id);
        ApplicationInfoDomain domain = appService.findApplicationById(form);
        Map<String, Object> map = new HashMap<>();
        map.put("app", domain);
        return map;
    }

    @RequestMapping(value = "/synConfig")
    @ResponseBody
    public Map<String, Object> synConfig(ApplicationInfoDomain form, @RequestParam(value = "id") String appId, HttpServletResponse response) {
        //FIXME:FIXED-诗昭
        Integer id = Integer.valueOf(appId);
        Map<String, Object> map = new HashMap<>();
        AllRuleInfoDomain ruleInfo = appService.getRuleInfo(appId);
        form.setId(id);
        ApplicationInfoDomain domain = appService.findApplicationById(form);

        /*
        新增从账号配置
        modify by cozi
        date 2019-06-11
         */
        String associatedAccountInfo = appService.associatedAccountById(id);
        AssociatedAccountInfo associatedAccountInfo1 = new Gson().fromJson(associatedAccountInfo, AssociatedAccountInfo.class);
        domain.setAssociatedAccount(associatedAccountInfo1);
        String autoSyncConfig = appService.autoSyncById(id);
        AutoSyncInfo autoSyncInfo = new Gson().fromJson(autoSyncConfig, AutoSyncInfo.class);
        domain.setAutoSync(autoSyncInfo);
        AppInfoBto appInfoBto = new AppInfoBto(domain);
        map.put("app", appInfoBto);
        if (ruleInfo == null) {
            ruleInfo = new AllRuleInfoDomain();
            map.put("ruleInfo", ruleInfo);
        } else {
            map.put("ruleInfo", ruleInfo);
        }
        return map;
    }

    @RequestMapping(value = "/edit")
    @ResponseBody
    public Map<String, Object> editApp(ApplicationInfoDomain form,
                                       @RequestParam(value = "id") String appId,
                                       @RequestParam(value = "value") String value,
                                       @RequestParam(value = "concatName") String concatName,
                                       @RequestParam("teamName") String teamName,
                                       HttpServletResponse response) {
        //FIXME:FIXED-诗昭
        Integer id = Integer.valueOf(appId);
        Map<String, Object> map = new HashMap<>();
        form.setId(id);
        List<ImageInfoDomain> list = imageInfoService.getImageList();
        ApplicationInfoDomain domain = appService.findApplicationById(form);
        map.put("imageList", list);
        map.put("app", domain);
        map.put("value", value);
        map.put("concatName", concatName);
        map.put("teamName", teamName);

        return map;
    }

    /**
     * modify by 田扛
     * modify time 2019年3月10日23:14:44
     * 跳转到授权用户界面并返回用户授权树
     * （数据隔离修改）
     */
    @CheckToken
    @RequestMapping(value = "/authorizationUser")
    @ResponseBody
    public Map<String, Object> authorizationUser(@RequestParam(value = "applicationId") String id, String currentPage,
                                                 @RequestParam(value = "queryName", required = false) String queryName, HttpServletRequest request) {
        //FIXME:FIXED-诗昭
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        List<String> accountNumbers = appService.getAuthUserAccountNumber(id);//获得该应用下所有授权到人的用户
        List<TreeNodesDomain> trees = appService.getUserTree(accountNumbers, companyId);
        Map<String, Object> map = new HashMap<>();
        map.put("trees", trees);
        return map;
    }

    /**
     * 获取授权部门的
     * create by 田扛
     * create time 2019年3月8日00:51:24
     */
    @RequestMapping(value = "/getAuthGroup")
    @ResponseBody
    public Map<String, Object> getAuthGroup(@RequestParam(value = "applicationId") String id, String currentPage,
                                            @RequestParam(value = "queryName", required = false) String queryName,
                                            HttpServletRequest request) {
        Map<String, Object> map = new HashMap();
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        List<TreeNodesDomain> trees = appService.getAuthGroupTree(id, companyId);
        map.put("trees", trees);
        return map;
    }

    /**
     * 获取所有授权安全组以及未授权的安全组
     */
    @RequestMapping(value = "/getAuthTeam")
    @ResponseBody
    public Map<String, Object> getAuthTeam(@RequestParam(value = "applicationId") String id, String currentPage,
                                           @RequestParam(value = "queryName", required = false) String queryName,
                                           HttpServletRequest request) {
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        Map<String, Object> map = appService.getAuthTeam(id, companyId);
        return map;
    }

    /**
     * 跳转到授权安全组界面
     */
    @RequestMapping(value = "/authorizationSecurityGroup")
    @ResponseBody
    public Map<String, Object> authorizationSecurityGroup(@RequestParam(value = "applicationId") String id) {
//        //FIXME:FIXED-诗昭
        Map<String, Object> map = new HashMap<>();
        return map;
    }

    /**
     * 跳转到授权部门界面
     */
    @RequestMapping(value = "/authorizationDepartment")
    @ResponseBody
    public Map<String, Object> authorizationDepartment(@RequestParam(value = "applicationId") String id) {
        //FIXME:FIXED-诗昭
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        return map;
    }

    /**
     * 修改应用信息
     * (数据隔离修改)
     * <p>
     * modify by cozi
     * date 2019-06-11
     * 新增从账号配置
     * 从账号密码配置
     * <p>
     * modify by cozi
     * date 2019-06-11
     * 新增自动同步配置
     */
    @CheckToken
    @RequestMapping(value = "/change")
    @ResponseBody
    public Map<String, Object> updateApp(HttpServletResponse response,
                                         ApplicationInfoDomain form,
                                         AssociatedAccountInfo associatedAccountInfo,
                                         AutoSyncInfo autoSyncInfo,
                                         HttpServletRequest request, HttpSession session) {
        //FIXME:FIXED-诗昭
        //System.out.println(form);
        ApplicationInfoDomain applicationInfoDomain= appService.findApplicationById(form);
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        redisClient.remove(CacheKey.getCacheApplicationList(companyId));
        if (form.getSecretKeyValueList() != null) {
            form.setConfigInfo(form.getConfigInfo());
        }
        if (StringUtils.isNotEmpty(form.getApplicationName())) {
            int count = appService.countApplication(form);
            if (count >= 1) {
                return failureResponse("应用名不能重复");
            }
        }
        form.setAssociatedAccount(new Gson().toJson(associatedAccountInfo));
        form.setAutoSync(new Gson().toJson(autoSyncInfo));

        if (!form.getAssociatedAccount().equals(applicationInfoDomain.getAssociatedAccount())){
            //根据从账号配置，进行设置
            setAssAccount(form.getId(), associatedAccountInfo, request);
        }
        if (form.getApplicationIndex() < 0) {
            return failureResponse("应用序号为正整数");
        } else {
            int number = appService.selectCountByAppIndex(form.getId(), form.getApplicationIndex(), companyId);
            if (number > 0) {
                return failureResponse("应用序号发生重复，请修改为其他序号");
            }
        }
        try {
            appService.updateApplicationInfo(form);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.APPLICATION_MAINTENANCE.getType(), "修改应用:" + form.getApplicationName());
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);

            UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
            String userName = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
            AppAuditInfo appAuditInfo = new AppAuditInfo(Integer.valueOf(form.getId()), userName, 2, "修改应用成功", companyId);
            appBehaviorPublistener.publish(appAuditInfo);
        } catch (Exception e) {
            e.printStackTrace();
            return failureResponse("修该应用信息失败");
        }
        return successResponse("修改应用信息成功");
    }

    /**
     * 根据从账号配置，进行设置
     *
     * @param id                    应用id
     * @param associatedAccountInfo 从账号配置
     * @return
     */
    public void setAssAccount(Integer id, AssociatedAccountInfo associatedAccountInfo, HttpServletRequest request) {
        if (id.intValue() > 0 && associatedAccountInfo != null) {
            List<NewAppSonAccountDomain> newAppSonAccountDomains = appService.getAssAccountByAppId(id);
            String applicatinId = appService.getApplicatinId(id);
            int flag = 6;
            String msg = "应用从账号更新：";
            //与主账号一致
            if (associatedAccountInfo.getAssPrimaryAccount() != null && associatedAccountInfo.getAssPrimaryAccount().equals(0)) {
                flag = 0;
                appService.editDownRule(id, 1);
                msg += "与主账号一致";
                //与主账号邮箱一致
            } else if (associatedAccountInfo.getAssEmail() != null && associatedAccountInfo.getAssEmail().equals(0)) {
                flag = 1;
                appService.editDownRule(id, 2);
                msg += "与主账号邮箱一致";
                //与主账号邮箱前缀一致
            } else if (associatedAccountInfo.getAssEmailPrefix() != null && associatedAccountInfo.getAssEmailPrefix().equals(0)) {
                flag = 2;
                appService.editDownRule(id, 3);
                msg += "与主账号邮箱前缀一致";
                //与主账号手机号一致
            } else if (associatedAccountInfo.getAssTelephone() != null && associatedAccountInfo.getAssTelephone().equals(0)) {
                flag = 3;
                appService.editDownRule(id, 4);
                msg += "与主账号手机号一致";
                //与主账号工号一致
            } else if (associatedAccountInfo.getAssWorkers() != null && associatedAccountInfo.getAssWorkers().equals(0)) {
                flag = 4;
                appService.editDownRule(id, 5);
                msg += "与主账号工号一致";
            } else if (associatedAccountInfo.getAssManual() != null && associatedAccountInfo.getAssManual().equals(0)) {
                flag = 5;
                msg += "手动配置";
            }
            if (flag != 6) {
                String assAccountIdByAppId = appService.getAssAccountIdByAppId(applicatinId);
                appService.delAssAccountIdByAppId(applicatinId, assAccountIdByAppId);
            }
            if (flag != 5 && flag != 6) {
                for (NewAppSonAccountDomain newAppSonAccountDomain : newAppSonAccountDomains) {
                    String mainAccountPwd = appService.getMainAccountPwd(newAppSonAccountDomain.getUuid());
                    boolean nFlag = false;

                    if (null == associatedAccountInfo.getAssPwdPrimaryAccount() || associatedAccountInfo.getAssPwdPrimaryAccount().equals(1)) {
                        mainAccountPwd = "";
                    }

                    SubAccountInfoDomain subAccountInfoDomain = new SubAccountInfoDomain();//从账号信息
                    subAccountInfoDomain.setPassword(mainAccountPwd);
                    subAccountInfoDomain.setAppClientId(applicatinId);

                    SubAccountMapDomain subAccountMapDomain = new SubAccountMapDomain();//主从账号关联信息
                    subAccountMapDomain.setUserId(newAppSonAccountDomain.getUuid());

                    switch (flag) {
                        case 0:
                            if (StringUtils.isNotEmpty(newAppSonAccountDomain.getAccountNumber())) {
                                subAccountInfoDomain.setSubAccount(newAppSonAccountDomain.getAccountNumber());
                                nFlag = true;
                            }
                            break;
                        case 1:

                            if (StringUtils.isNotEmpty(newAppSonAccountDomain.getMail())) {
                                subAccountInfoDomain.setSubAccount(newAppSonAccountDomain.getMail());
                                nFlag = true;
                            }
                            break;
                        case 2:
                            if (StringUtils.isNotEmpty(newAppSonAccountDomain.getMailPrefix())) {
                                subAccountInfoDomain.setSubAccount(newAppSonAccountDomain.getMailPrefix());
                                nFlag = true;
                            }
                            break;
                        case 3:
                            if (StringUtils.isNotEmpty(newAppSonAccountDomain.getPhoneNumber())) {
                                subAccountInfoDomain.setSubAccount(newAppSonAccountDomain.getPhoneNumber());
                                nFlag = true;
                            }

                            break;
                        case 4:
                            if (StringUtils.isNotEmpty(newAppSonAccountDomain.getJobNo())) {
                                subAccountInfoDomain.setSubAccount(newAppSonAccountDomain.getJobNo());
                                nFlag = true;
                            }
                            break;
                    }
                    if (nFlag) {
                        try {
                            appService.editSubAccount(subAccountInfoDomain);
                            subAccountMapDomain.setSubId(subAccountInfoDomain.getId());
                            appService.editSubAccountMap(subAccountMapDomain);
                        } catch (Exception e) {
                            e.printStackTrace();
                            logger.error("enter AppController.setAssAccount() but insert faild,subAccountInfoDomain=[{}]=,subAccountMapDomain=[{}]==" + new Object[]{
                                    subAccountInfoDomain, subAccountMapDomain
                            });
                        }
                    }
                }
                //添加应用日志
                String companyId = ConstantsCMP.getSessionCompanyId(request);
                try {
                    UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
                    String userName = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
                    AppAuditInfo appAuditInfo = new AppAuditInfo(id, userName, 4, msg, companyId);
                    appBehaviorPublistener.publish(appAuditInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @RequestMapping(value = "/updateInfomation")
    @ResponseBody
    public Map<String, Object> updateInfomation(HttpServletResponse response, ApplicationInfoDomain form, HttpServletRequest request) {
        //FIXME:FIXED-诗昭
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        if (form.getSecretKeyValueList() != null) {
            form.setConfigInfo(form.getConfigInfo());
        }
        try {
            appService.updateApplicationInfo(form);
        } catch (Exception e) {
            e.printStackTrace();
            return failureResponse("修该应用信息失败");
        }
        try {
            UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
            String userName = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
            AppAuditInfo appAuditInfo = new AppAuditInfo(Integer.valueOf(form.getId()), userName, 2, "修改应用成功", companyId);
            appBehaviorPublistener.publish(appAuditInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return successResponse("修改应用信息成功");
    }

    /**
     * 获取所有已对接的应用
     */
    @RequestMapping(value = "/applist")
    @ResponseBody
    public Map<String, Object> applist(@RequestParam(value = "currentPage") String currentPage,
                                       @RequestParam(value = "queryName") String queryName) {
        //FIXME:FIXED-诗昭
        PageDomain pageDomain = appService.queryAllApplications(currentPage, queryName);
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotBlank(queryName)) {
            map.put("queryName", queryName);
        }
        System.out.println(pageDomain.toString());
        map.put("pageDomain", pageDomain);
        return map;
    }

    /**
     * 账号下发规则
     */
    @RequestMapping(value = "/sonAdd")

    public void sonAdd(HttpServletResponse response, @RequestParam(value = "applicationId") String applicationId, AllRuleInfoDomain allRuleInfoDomain, RuleInfoDomain ruleInfoDomain, HttpServletRequest request) {
        if (ruleInfoDomain != null) {
            String ruleValue = JSON.toJSONString(ruleInfoDomain);
            allRuleInfoDomain.setRuleValue(ruleValue);
        }

        int count = appService.getaccountSonRule(applicationId);
        if (count == 0) {
            appService.insertSonRule(allRuleInfoDomain);
            ResponseUtils.customSuccessResponse(response, "应用规则成功");
            return;
        } else {
            appService.undateSonRule(allRuleInfoDomain);
            ResponseUtils.customSuccessResponse(response, "应用规则已更新");
            return;
        }

    }

    /**
     * 应用从账号规则添加
     *
     * @param response
     * @param applicationId
     * @param allRuleInfoDomain
     * @param request
     */
    @RequestMapping(value = "/ruleAdd")
    public void ruleAdd(HttpServletResponse response,
                        @RequestParam(value = "applicationId") String applicationId,
                        AllRuleInfoDomain allRuleInfoDomain,
                        HttpServletRequest request) {
        if (allRuleInfoDomain.getSubSynLocalDomainList() != null) {
            String ruleValue = JSON.toJSONString(allRuleInfoDomain.getSubSynLocalDomainList());
            allRuleInfoDomain.setRuleValue(ruleValue);
        }
        int count = appService.getaccountSonRule(applicationId);
        if (count == 0) {
            appService.insertSonRule(allRuleInfoDomain);
            ResponseUtils.customSuccessResponse(response, "应用规则成功");
            return;
        } else {
            appService.undateSonRule(allRuleInfoDomain);
            ResponseUtils.customSuccessResponse(response, "应用规则已更新");
            return;
        }

    }

    /**
     * 腾讯企业邮插入从账号的命名规则
     *
     * @param response
     * @param applicationId
     * @param allRuleInfoDomain
     * @param subSynLocalDomainList
     * @param request
     */
    @RequestMapping(value = "/tencentRuleAdd")

    public void tecentRuleAdd(HttpServletResponse response, @RequestParam(value = "applicationId") String applicationId, AllRuleInfoDomain allRuleInfoDomain, List<SubSynLocalDomain> subSynLocalDomainList, HttpServletRequest request) {

        if (subSynLocalDomainList != null) {
            String ruleValue = JSON.toJSONString(subSynLocalDomainList);
            allRuleInfoDomain.setRuleValue(ruleValue);
        }

        int count = appService.getaccountSonRule(applicationId);
        if (count == 0) {
            ResponseUtils.customSuccessResponse(response, "应用规则成功");
            return;
        } else {
            appService.undateSonRule(allRuleInfoDomain);
            ResponseUtils.customSuccessResponse(response, "应用规则已更新");
            return;
        }

    }

    /*
     * 应用列表
     * （数据隔离修改）
     *
     * */
    //@CheckToken
    @RequestMapping(value = "/list", params = "json", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getList(ApplicationInfoDomain form, DataGridModel pageModel, HttpServletRequest request, HttpServletResponse response) {
        if (logger.isDebugEnabled()) {
            logger.debug("turn to add on the NewUserInfoController.queryData, form", new Object[]{form.toString()});
        }
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        form.setCompanyId(companyId);
        return appService.getPageList(form, pageModel);
    }

    /**
     * 从应用列表中删除应用，同时删除和该应用相关的人，部门，安全组
     *
     * @param id
     * @param request
     * @param
     * @return
     * @author cozi
     * @date 2019-06-12
     * @version 1.5.5
     */
    @RequestMapping(value = "/delApplication", method = RequestMethod.POST)
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> delApplication(@RequestParam(value = "id") int id, HttpServletRequest request, HttpSession session) {
        String companyId = sessionService.getCompanyUUid(request.getSession());
        redisClient.remove(CacheKey.getCacheApplicationList(companyId));
        if (id > 0 && StringUtils.isNotEmpty(companyId)) {
            try {
                appService.delApplication(id, companyId);
            } catch (Exception e) {
                e.printStackTrace();
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return NewReturnUtils.failureResponse("刪除失敗!");
            }
        }
        try {
            UserInfoDomain userInfoDomain = (UserInfoDomain) request.getSession().getAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);
            String userName = userInfoDomain.getUserName() + "(" + userInfoDomain.getAccountNumber() + ")";
            AppAuditInfo appAuditInfo = new AppAuditInfo(id, userName, 2, "删除应用成功", companyId);
            appBehaviorPublistener.publish(appAuditInfo);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.APPLICATION_MAINTENANCE.getType(), "删除应用成功");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo, session);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NewReturnUtils.successResponse("删除应用成功");


    }

    /**
     * 跳转到用户账号详细信息的页面
     *
     * @param appId
     * @return
     */
    @RequestMapping(value = "/userDetail")
    @ResponseBody
    public Map<String, Object> userDetail(@RequestParam(value = "appId") String appId) {
        //FIXME:FIXED-诗昭
        Map<String, Object> map = new HashMap<>();
        map.put("appId", appId);
        return map;
    }

    /**
     * 根据appId查询能看到该应用的所有用户的详细信息
     *
     * @param request
     * @param appId
     * @return
     */
    @RequestMapping(value = "/accountInfo", params = "json", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> subAccountDetail(HttpServletRequest request,
                                                @RequestParam(value = "applicationId") String appId,
                                                @RequestParam(value = "queryName", required = false) String queryName,
                                                @RequestParam(value = "sord", required = false) String sord,
                                                @RequestParam(value = "sidx", required = false) String sidx,
                                                DataGridModel pageModel) {
        ApplicationSubAccountModel form = new ApplicationSubAccountModel();
        form.setApplicationId(Integer.valueOf(appId));
        form.setQueryName(queryName);
        form.setSord(sord);
        form.setSidx(sidx);

        return appService.getAccountPageListByAppId(form, pageModel);

    }

    @RequestMapping(value = "/imgUpload", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> imgUpload(@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String companyId = sessionService.getCompanyUUid(request.getSession());
        logger.info("enter AppController.imgUpload;filename:" + file.getOriginalFilename());
        byte[] imgByte = file.getBytes();
        for (int i = 0; i < imgByte.length; ++i) {
            //异常数据调整
            if (imgByte[i] < 0) {
                imgByte[i] += 256;
            }
        }
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMddHHmmss");
        String fileName = file.getOriginalFilename();
        int pointIndex = fileName.lastIndexOf(".");
        String endStr = fileName.substring(pointIndex);
        String imgName = simpleDateFormat.format(date) + endStr;
        SystemConfigInfo systemConfigInfo = systemConfigService.getSystemConfigInfo(companyId);
        //Boolean flag = uploadImg(imgProperties.getSaveDir(), imgName, imgByte);
        Boolean flag = uploadImg("/data/html/", imgName, imgByte);
        Map<String, Object> map = new HashMap<>();
        logger.info("leave from AppController.imgUpload");
        if (flag) {
            map.put("status", 0);
            // map.put("imgName", imgProperties.getServerHost() + imgProperties.getAccessAddress() + imgName);
            map.put("imgName", systemConfigInfo.getImageHost() + "/" + imgName);
        } else {
            map.put("status", 1);
        }
        return map;
    }

    @RequestMapping(value = "/common", params = "json")
    @ResponseBody
    public List<ApplicationSelectModel> applicationSelect(HttpServletResponse response) {
        return appService.queryAppSelect();
    }


}
