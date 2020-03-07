package com.portal.controller;





import com.portal.domain.ApplicationInfoDomain;
import com.portal.domain.UserInfoDomain;
import com.portal.service.ApplicationService;
import com.portal.service.UserInfoService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping(value = "/cipher/application")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserInfoService userInfoService;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    /**
      * 子账号推荐功能
      * */
    @RequestMapping(value = "/recomendSubAccount", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> recomendSubAccount(HttpServletRequest request, Model model,
                                                  @RequestParam(value = "username") String username,
                                                  @RequestParam(value = "companyUuid") String companyUuid,
                                                  @RequestParam(value = "clientId", required = false) String clientId) {
        Map<String, Object> map = new HashMap<>();
        UserInfoDomain userInfoDomain = userInfoService.getUserInfo(username,companyUuid);
        if(null==userInfoDomain){
            map.put("return_code",1);
            map.put("return_msg","用户不存在");
            return map;
        }
        if (StringUtils.isNotEmpty(clientId)) {
            ApplicationInfoDomain applicationInfoDomain = applicationService.queryApplication(clientId);
            if (null != applicationInfoDomain&& StringUtils.isNotEmpty(applicationInfoDomain.getAccountType())) {
                Integer ruleType = Integer.valueOf(applicationInfoDomain.getAccountType());
                logger.info("ruleType--------"+ruleType);
                switch (ruleType) {
                    case 1:
                        map.put("subAcount", userInfoDomain.getAccountNumber());
                        break;
                    case 2:
                        map.put("subAcount", userInfoDomain.getPhoneNumber());
                        break;
                    case 3:
                        map.put("subAcount", userInfoDomain.getMail());
                        break;
                    case 4:
                        String mail = userInfoDomain.getMail().substring(0, userInfoDomain.getMail().indexOf("@"));
                        map.put("subAcount", mail);
                        break;
                    default:
                        break;
                }
            }
        }

        map.put("return_code",0);
        return map;
    }

}
