package cipher.console.oidc.controller;

import cipher.console.oidc.common.HttpKey;
import cipher.console.oidc.domain.web.AdminUser;
import cipher.console.oidc.enums.ResultCode;
import cipher.console.oidc.service.CompanyService;
import cipher.console.oidc.service.RegisterApprovalService;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 * create by shizhao at 2020-2-27
 *
 * @author: shizhao
 * @since: 2020-2-27 15:22
 */
@Controller
@RequestMapping("/cipher/user")
public class AdminCreateController extends BaseController{

    @Autowired
    private RegisterApprovalService registerApprovalService;

    private static Logger logger= LoggerFactory.getLogger(AdminCreateController.class);

    @Autowired
    private CompanyService companyService;

    public static final String PUBLIC_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApb+GNNCtCS9FXG5RDIYTT1t29w/aukBRmKGst2ZG9N2G7yhC0KbvQGZp5x96Q4zRGSrmXyF5249FWWxqSYNW3kZ8ZUc3HFBmZMJHmKCsLp55Cbi+SVwTPz3a6f7o88qytamweeVtsx1cMogag+FULRY4Vj4DeB81W4NhQntSdX7JHdJWt2ZeCkJs7Dgy3jfo7UqiBwSNQp4qkm1ZTk6g8n+t1mHREAZSnO71/j3IDxEMPE3OQ8sNXsEEonsWRT5HfETXBiyGdLVgzlnzh1i3KiRC93Kr6rf5XngGnmBUV1R6BCkzBMNwKzb9vVSFboVpwFcoTfVQ0aoXB/XjTJZIpQIDAQAB";

    @RequestMapping(value = "/createAdmin")
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public Map<String,Object> createAdmin(AdminUser adminUser, String sign, String timestamp){
        Map<String,String> map=new HashMap<>();
        map.put("bizcontent",adminUser.getUuid()+timestamp);
        map.put("sign",sign);
        boolean check= false;
        try {
            check = AlipaySignature.rsaCheckV2(map,PUBLIC_KEY , "utf-8","RSA2");
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (check == false){
            return sendBaseErrorMap(ResultCode.TOKEN_CHECKED_FAILD);
        }
        registerApprovalService.createAdminUser(adminUser);
        try {
            companyService.insertCompanyInfo(adminUser);
            companyService.initData(adminUser.getCompanyUuid());
        } catch (Exception e) {
            logger.error("error","插入失败");
            logger.error(e.getMessage(),e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return sendBaseErrorMap(ResultCode.COMPANY_INSERT_ERROR);
        }
        return sendBaseNormalMap();
    }
}
