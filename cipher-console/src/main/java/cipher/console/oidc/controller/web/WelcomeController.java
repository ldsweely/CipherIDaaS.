package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.domain.web.ApplicationInfo;
import cipher.console.oidc.domain.web.NewUserInfo;
import cipher.console.oidc.domain.web.UserInfoDomain;
import cipher.console.oidc.mapper.NewUserMapper;
import cipher.console.oidc.service.ApplicationAuditInfoService;
import cipher.console.oidc.service.UserGroupService;
import cipher.console.oidc.service.UserService;
import cipher.console.oidc.service.WelcomeUserService;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.MapUtil;
import cipher.console.oidc.util.aes.AES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * @Author: zt
 * @Date: 2018/5/30 9:35p
 */
@Controller
@RequestMapping(value = "/cipher/welcome")
@EnableAutoConfiguration
public class WelcomeController {


    @Autowired
    private UserGroupService userGroupService;


    @Autowired
    private ApplicationAuditInfoService applicationAuditInfoService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private WelcomeUserService welcomeUserService;
    @Autowired
    private NewUserMapper newUserMapper;



    private static final Logger LOGGER= LoggerFactory.getLogger(WelcomeController.class);


    /*
    *  首页信息
    * （数据隔离修改）
    * */
    @RequestMapping(value = "/draw", method = RequestMethod.POST)
    @ResponseBody
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDrawData( @RequestParam(value = "companyUUid") String companyId,HttpServletRequest request, HttpServletResponse response) {
        LOGGER.debug("enter WelcomeController.getDrawData;");
        ApplicationInfo form = new ApplicationInfo();
        DataGridModel pageModel = new DataGridModel();
        form.setCompanyId(companyId);
        pageModel.setCurrentRow(0);
        pageModel.setRows(50);
        List<ApplicationInfo> list = applicationAuditInfoService.getApplicationList(form,pageModel);
        if(CollectionUtils.isEmpty(list)){
            return null;
        }
        List<Integer> data=new ArrayList<>();
        List<Integer> data2=new ArrayList<>();
        List<String> nameList=new ArrayList<>();

        for(ApplicationInfo applicationInfo:list){
            data.add(applicationInfo.getCountBySev());
            data2.add(applicationInfo.getCountByMon());
            nameList.add(applicationInfo.getApplicationName());
        }

        Map<String, Object> map2 = new HashMap<>();
        map2.put("timeList", nameList);
        map2.put("data", data);
        map2.put("data2", data2);
        return map2;
    }


    /*
    * 首页获取用户信息
    * （数据隔离修改）
    *
    * */

    @RequestMapping(value = "/userInfo", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getUserCount(HttpServletRequest request, @RequestParam(value = "companyUUid") String companyId, DataGridModel page
    ) {
        UserInfoDomain user=getUserInfoByToken(request);
        String uuid="";
        if(null!=user) {
             companyId = user.getCompanyId();
             uuid= user.getUuid();
        }
        Map<String, Object> map = new HashMap<>();
        //无部门用户
        NewUserInfo newUserInfo=new NewUserInfo();
        newUserInfo.setCompanyId(companyId);
        int noGroupCount=(Integer) welcomeUserService.getNoGroupUserList(newUserInfo,page).get("total");
        map.put("noGroupCount", noGroupCount);
        //锁定用户
        int lockCount=getLockUserList(newUserInfo);
        map.put("lockCount", lockCount);
        //未登录用户
        int noLoginCount=(Integer) welcomeUserService.getNoLoginUserList(newUserInfo,page).get("total");
        map.put("noLoginCount", noLoginCount);
        UserInfoDomain userInfoDomain=userService.getUserByAccountNumber(uuid);
        if(null!=userInfoDomain){
            map.put("userName", userInfoDomain.getUserName());
        }
        return map;
    }


    public  int  getLockUserList(NewUserInfo form) {
        List<NewUserInfo> list = newUserMapper.getNewAllList(form);
        List<NewUserInfo> newList = new ArrayList<>();
        for (NewUserInfo newUserInfo : list) {
            boolean result = redisTemplate.hasKey(CacheKey.getUserLoginFailedInfoCacheKey(newUserInfo.getUuid()));
            if (result == true) {
                newList.add(newUserInfo);
            }
        }
        if(null==newList){
            return 0;
        }else{
           return newList.size();
        }

    }


    /**
     * 根据在线用户的key列表，查询出所有在线用户的用户名列表
     * 该接口已废弃
     * @param keyList
     * @return
     */
    private List<String> queryOnlineAccountList(Set<String> keyList) {
        List<String> nameList = new ArrayList<>();
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        for (String key : keyList) {
            nameList.add(valueOperations.get(key));
        }
        return nameList;
    }




    public  UserInfoDomain getUserInfoByToken(HttpServletRequest request){
        UserInfoDomain user=new UserInfoDomain();
        String cipherparam = request.getParameter("ticket");
        if(StringUtils.isEmpty(cipherparam)){
            return null;
        }
        String param=AES.decryptFromBase64(cipherparam,ConstantsCMP.AES_KEY);
        //将字符串转换成map对象
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        //将对象放到map中
        parameterMap = MapUtil.getStringToMap(param);
        String uuid = (String) parameterMap.get("uuid");
        String companyId = (String) parameterMap.get("companyUUid");
        user.setUuid(uuid);
        user.setCompanyId(companyId);
        return user;
    }
}
