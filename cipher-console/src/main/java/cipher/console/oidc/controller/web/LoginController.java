package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.domain.web.UserInfoDomain;
import cipher.console.oidc.domain.web.UserSessionResp;
import cipher.console.oidc.mapper.UserMapper;
import cipher.console.oidc.model.UserPwdModel;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.service.UserService;
import cipher.console.oidc.util.CookieUtils;
import cipher.console.oidc.util.MapUtil;
import cipher.console.oidc.util.NumberUtil;
import cipher.console.oidc.util.ResponseUtils;
import cipher.console.oidc.util.aes.AES;
import cipher.console.oidc.util.aes.AesKey;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * @Author: zt
 * @Date: 2018/5/31 16:03
 */
@Controller
@RequestMapping(value = "/cipher")
public class LoginController {






    @Autowired
    private RedisClient redisClient;


    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;


    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(OidcCallback.class);

    private static final String SECRET_KEY = "cipher_2018";

    @Autowired
    private UserMapper userMapper;


    @RequestMapping(value = "/console/setSession", method = RequestMethod.POST)
    @ResponseBody
    public void indexPage(HttpServletRequest request, HttpSession session, HttpServletResponse response) {
        //TODO
        /*String userName = ConstantsCMP.getSessionUser(request);
        if (StringUtils.isNotEmpty(userName)) {
            UserInfoDomain userInfoDomain = userService.getUserByAccountNumber(userName);
            if (null != userInfoDomain) {
                session.setAttribute("account", userName);
            }*/
            ResponseUtils.customSuccessResponse(response, "操作成功");
        //}

    }

    @RequestMapping(value = "/checkpwd", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> checkUser(@RequestParam(value = "userName") String userName,
                                         @RequestParam(value = "pwd") String pwd,
                                         @RequestParam(value = "companyUUid") String companyId,
                                         HttpServletResponse response,HttpServletRequest request,
                                         HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        UserInfoDomain domain = userMapper.queryPwd(userName,companyId);
        if (null == domain) {
            map.put("return_code", ConstantsCMP.Code.FAIL);
            map.put("return_msg", "用户账号不存在");
            return map;
        }
        if (domain.getIsAdmin() == 0) {
            map.put("return_code", ConstantsCMP.Code.FAIL);
            map.put("return_msg", "你不是管理员，没有权限登录");
            return map;
        }
        String oldpwd = AES.decryptFromBase64(domain.getPassword(), AesKey.AES_KEY);
        if (!pwd.equals(oldpwd)) {
            map.put("return_code", ConstantsCMP.Code.FAIL);
            map.put("return_msg", "密码错误");
            return map;
        }
        session.setAttribute(ConstantsCMP.CIPHER_UUID_INFO, domain.getUuid());
        session.setAttribute(ConstantsCMP.CIPHER_CONSOLE_COMPANY_SESSION_INFO,companyId);
        session.setAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO,domain);
        companyId=ConstantsCMP.getSessionCompanyId(request);
        String uuid=ConstantsCMP.getCipherUuidInfo(request);
        logger.info("companyId---------"+companyId+"------uuid---------"+uuid);
        UserSessionResp userSessionResp =sessionService.setUserLoginInfo(domain.getUuid());
        response.setHeader("ticket",userSessionResp.getTicket());
        redisClient.put(CacheKey.getCacheKeyCipherConsoleUserInfo(uuid),userSessionResp.getTicket());
        map.put("return_code", ConstantsCMP.Code.SUCCESS);
        map.put("return_msg", "登录成功");
        return map;
    }








    @RequestMapping(value = "/loginOut" , method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> loginOut(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        Map<String, Object> map = new HashMap<>();
        String uuid = ConstantsCMP.getCipherUuidInfo(request);
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        if (org.apache.commons.lang3.StringUtils.isNotEmpty(uuid)||StringUtils.isNotEmpty(companyId)) {
            session.removeAttribute(uuid);
            session.removeAttribute(companyId);
        }
        map.put("return_code", ConstantsCMP.Code.SUCCESS);
        map.put("return_msg", "退出成功");
        return map;
    }


}
