package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.domain.web.AdminBehaviorInfo;
import cipher.console.oidc.domain.web.LoginFailInfo;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.AdminBehaviorInfoService;
import cipher.console.oidc.service.LoginFailService;
import cipher.console.oidc.token.CheckToken;
import cipher.console.oidc.util.IpUtil;
import cipher.console.oidc.util.ResponseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

/**
 * Created by 95744 on 2018/8/23.
 */

@Controller
@RequestMapping(value = "/cipher/loginfail")
@EnableAutoConfiguration
public class LoginFailController {

    private static final Logger logger = LoggerFactory.getLogger(LoginFailController.class);

    @Autowired
    private LoginFailService loginFailService;



    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;




    /**
     * 身份认证异常登录信息
     * （数据隔离修改）
     */
    @CheckToken
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ResponseBody
    public LoginFailInfo queryDataInfo(HttpServletRequest  request) {
        logger.debug("enter LoginFailController.queryDataInfo");
        String companyId=ConstantsCMP.getSessionCompanyId(request);

        LoginFailInfo loginFailInfo=new LoginFailInfo();
        loginFailInfo.setCompanyId(companyId);
        return loginFailService.getLoginFailInfo(loginFailInfo);
    }

    /**
     * 身份认证异常登录信息
     * （数据隔离修改）
     */
    @CheckToken
    @RequestMapping(value = "/update")
    @ResponseBody
    public void updateLoginFailInfo(HttpServletResponse response, HttpServletRequest request, LoginFailInfo form, HttpSession session) {
        String companyId=ConstantsCMP.getSessionCompanyId(request);
        form.setCompanyId(companyId);
        logger.debug("enter LoginFailController.updateLoginFailInfo form" + form.toString());
        LoginFailInfo loginFailInfo =  loginFailService.getLoginFailInfo(form);
        try {
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.STRATEGY_MAINTENANCE.getType(),  "更新登录失败控制");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ObjectUtils.isEmpty(loginFailInfo)) {
            loginFailService.insertLoginFailInfo(form);
            ResponseUtils.customSuccessResponse(response, "保存配置信息成功");
        }
        try {
            loginFailService.updateLoginFailInfo(form);
            ResponseUtils.customSuccessResponse(response, "更新配置信息成功");
        } catch (Exception e) {
            logger.error("enter LoginFailController.insertLoginFailInfo; Error:" + e.getCause());
            ResponseUtils.customFailueResponse(response, "更新配置失败，请稍后重试");
            e.printStackTrace();
        }
    }

}
