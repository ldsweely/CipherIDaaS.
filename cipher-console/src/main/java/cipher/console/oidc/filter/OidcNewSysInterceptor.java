package cipher.console.oidc.filter;


import cipher.console.oidc.common.CacheKey;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.domain.web.UserInfoDomain;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.UserService;
import cipher.console.oidc.util.CookieUtils;
import cipher.console.oidc.util.MapUtil;
import cipher.console.oidc.util.NumberUtil;
import cipher.console.oidc.util.aes.AES;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Component
public class OidcNewSysInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private UserService userService;

    private String cookieStr;


    private static final Logger logger = LoggerFactory.getLogger(OidcNewSysInterceptor.class);

    private static Set<String> uriSet = new HashSet<>();


    static {
        uriSet.add("/callBack");
        //金山云登录
        uriSet.add("/login/do");
        //金山云验证密码
        uriSet.add("/login/checkpwd");
        uriSet.add("/cipher/userbehavior/exportExcle");
        uriSet.add("/cipher/adminbehavior/exportExcle");
        uriSet.add("/cipher/download/subexport");
        uriSet.add("/cipher/subaccount/upload");
        uriSet.add("/cipher/vistorLoginLog/downloadExcelLog");
        uriSet.add("/cipher/subaccount/downloadReturnExcel");
        uriSet.add("/cipher/applyAudit/exportExcle");
        uriSet.add("/cipher/app/imgUpload");
        uriSet.add("/cipher/newImportbak/userexport");
        //金山云单点登录
        uriSet.add("/sso/");
        uriSet.add("/sso");
        //金山云注销登录
        uriSet.add("/logout/do");
        uriSet.add("/cipher/checkpwd");
        uriSet.add("/cipher/ssoLogin/createToeken");
        uriSet.add("/cipher/ssoLogin/checkToken");

        uriSet.add("/cipher/welcome/userInfo");
        uriSet.add("/cipher/menu/getAllMenuList");
        uriSet.add("/cipher/welcome/draw");
        uriSet.add("/cipher/console/setSession");
        uriSet.add("/cipher/newImportbak/import");
        uriSet.add("/cipher/loginOut");
        uriSet.add("/cipher/newImportbak/downloadReturnExcel");
        uriSet.add("/cipher/newImportbak/updateUserInfoExport");
        uriSet.add("/cipher/newImportbak/updateUserImport");
        uriSet.add("/cipher/newImportbak/downloadErrorExcel");
        uriSet.add("/cipher/obtain/copyright");
        uriSet.add("/cipher/equipBehavior/exportExcle");
        uriSet.add("/cipher/saml/metadata");
        uriSet.add("/cipher/user/createAdmin");

    }


    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (uriSet.contains(httpServletRequest.getRequestURI())) {
            return true;
        }
        HttpSession session = httpServletRequest.getSession();

        if (null == redisClient) {
            BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext(httpServletRequest.getServletContext());
            redisClient = (RedisClient) factory.getBean("redisClient");
        }

        if (null == userService) {
            BeanFactory factory = WebApplicationContextUtils.getRequiredWebApplicationContext(httpServletRequest.getServletContext());
            userService = (UserService) factory.getBean("userService");
        }

        String cipherparam = httpServletRequest.getParameter("ticket");
        if (StringUtils.isEmpty(cipherparam)) {
            return sessionExpire(httpServletResponse, "用户信息异常，请重新登录");
        }
        String uuid = getUuid(cipherparam);

        if (StringUtils.isEmpty(uuid)) {
            return sessionExpire(httpServletResponse, "用户信息异常，请重新登录");
        }


        String cookieRedis = "";

        String cookieStr = "";


        synchronized (redisClient){
            Object obj = redisClient.get(CacheKey.getCacheKeyCipherConsoleUserInfo(uuid));
            redisClient.remove(CacheKey.getCacheKeyCipherConsoleUserInfo(uuid));
            String value = null;
            if (null != obj) {
                value = CookieUtils.writeCookie(httpServletRequest, httpServletResponse, ConstantsCMP.CONSOLE_USER_COOKIE, uuid);
                UserInfoDomain userInfoDomain = userService.getUserInfoByCompany(uuid);
                session.setAttribute(ConstantsCMP.CIPHER_CONSOLE_COMPANY_SESSION_INFO, userInfoDomain.getCompanyId());
                session.setAttribute(ConstantsCMP.CIPHER_UUID_INFO, uuid);
                session.setAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO, userInfoDomain);
            /*    Cookie cookie = new Cookie("JSESSIONID",session.getId());
                cookie.setPath(httpServletRequest.getContextPath()+"/");
                cookie.setMaxAge(12*60*60);
                httpServletResponse.addCookie(cookie);
           */
            }
            if (StringUtils.isNotEmpty(value)){
                redisClient.put(CacheKey.getCacheKeyUserCookieValue(uuid),value,5);
        }
            cookieRedis = (String) redisClient.get(CacheKey.getCacheUserConsoleCookieInfo(uuid));

            cookieStr = redisClient.get(CacheKey.getCacheKeyUserCookieValue(uuid))==null?
                    CookieUtils.getCookie(httpServletRequest, ConstantsCMP.CONSOLE_USER_COOKIE):(String)redisClient.get(CacheKey.getCacheKeyUserCookieValue(uuid));
        }

        //TODO


        Object user = session.getAttribute((String) ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO);

        logger.info("cookieStr=======" + cookieStr + "=====cookieRedis======" + cookieRedis + "=====uuid=======" + uuid);
        if (null == user) {

            return sessionExpire(httpServletResponse, "登录超时，请重新登录");
        }
        if (null != cookieRedis && StringUtils.isNotEmpty(cookieStr) && !cookieStr.equals(cookieRedis)) {
            return sessionExpire(httpServletResponse, "账号已在其他位置登录，请重新登录");
        }

        return true;
    }


    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
    }

    public boolean sessionExpire(HttpServletResponse httpServletResponse, String msg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("return_code", ConstantsCMP.SESSION_EXPIRE);
        jsonObject.put("return_msg", msg);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setContentType("application/json; charset=utf-8");
        try {
            PrintWriter out = httpServletResponse.getWriter();
            out.append(jsonObject.toJSONString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getUuid(String param) {
        String cipherparam = AES.decryptFromBase64(param, ConstantsCMP.AES_KEY);
        //将字符串转换成map对象
        Map<String, Object> parameterMap = new HashMap<String, Object>();
        //将对象放到map中
        parameterMap = MapUtil.getStringToMap(cipherparam);
        String uuid = (String) parameterMap.get("uuid");
        if (StringUtils.isNotEmpty(uuid)) {
            return uuid;
        }
        return null;
    }


    public synchronized String getcookieStr(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, String uuid, HttpSession session) {
        String cipherParam = NumberUtil.createRandomCharData(10);
        UserInfoDomain userInfoDomain = userService.getUserInfoByCompany(uuid);
        CookieUtils.removeCookie(httpServletRequest, httpServletResponse);
        session.setAttribute(ConstantsCMP.CIPHER_CONSOLE_COMPANY_SESSION_INFO, userInfoDomain.getCompanyId());
        session.setAttribute(ConstantsCMP.CIPHER_UUID_INFO, uuid);
        session.setAttribute(ConstantsCMP.CIPHER_CONSOLE_USER_SESSION_INFO, userInfoDomain);
        redisClient.remove(CacheKey.getCacheKeyCipherConsoleUserInfo(uuid));
        CookieUtils.writeCookie(httpServletRequest, httpServletResponse, ConstantsCMP.CONSOLE_USER_COOKIE, cipherParam);
        cookieStr = CookieUtils.getCookie(httpServletRequest, ConstantsCMP.CONSOLE_USER_COOKIE);
        redisClient.put(CacheKey.getCacheUserConsoleCookieInfo(uuid), cipherParam);
        String cookieRedis = (String) redisClient.get(CacheKey.getCacheUserConsoleCookieInfo(uuid));
        System.out.println("cookieStr========" + cookieStr + "=====cookieRedis======" + cookieRedis);
        return cookieStr;
    }

}
