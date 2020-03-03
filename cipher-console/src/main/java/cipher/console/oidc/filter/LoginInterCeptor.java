package cipher.console.oidc.filter;

import cipher.console.oidc.common.ConstantsCMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;
/**
 * session过期拦截器
 * 当session过期时跳转到登录页
 */
public class LoginInterCeptor implements HandlerInterceptor {


    Logger logger = LoggerFactory.getLogger(LoginInterCeptor.class);
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
        //金山云单点登录
        uriSet.add("/sso/");
        uriSet.add("/sso");
        //金山云注销登录
        uriSet.add("/logout/do");
        uriSet.add("/cipher/checkpwd");
        uriSet.add("/cipher/ssoLogin/createToeken");
        uriSet.add("/cipher/ssoLogin/checkToken");
        uriSet.add("/cipher/user/createAdmin");
    }


    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (uriSet.contains(httpServletRequest.getRequestURI())) {
            return true;
        }
        HttpSession session = httpServletRequest.getSession(false);
        String uuid=(String) session.getAttribute(ConstantsCMP.CIPHER_UUID_INFO);
        String companyUUid = (String) session.getAttribute(ConstantsCMP.CIPHER_CONSOLE_COMPANY_SESSION_INFO);
        String ticket = httpServletRequest.getParameter("ticket");
        logger.info("uuid-----------"+uuid+"--------companyUUid-------"+companyUUid);
        if (null==ticket) {
            logger.info(httpServletRequest.getRequestURI()+"进入session为空");
            httpServletResponse.flushBuffer();
            return false;
        }
        return true;

    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }


}
