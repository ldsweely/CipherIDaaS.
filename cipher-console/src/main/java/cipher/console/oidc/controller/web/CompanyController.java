package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.Constants;
import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.controller.BaseController;
import cipher.console.oidc.service.CompanyService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO:
 * create by shizhao at 2020-3-4
 *
 * @author: shizhao
 * @since: 2020-3-4 10:12
 */
@RequestMapping(value = "/cipher/company")
@Controller
public class CompanyController extends BaseController {

    @Autowired
    private CompanyService companyService;


    public static void main(String... args){
        String url ="http://cipherchina.cipherchina.com:8645";
        int s=StringUtils.indexOf(url,".cipherchina.com");
        String domain=StringUtils.substring(url,0,s+16);
        System.out.println(domain);
    }
    /**
     * 获取companyUUid
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "/getCompanyUUid")
    @ResponseBody
    public Map<String,Object> getCompanyUUid(HttpServletRequest request, HttpServletResponse response,String domain){
        Map<String,Object> resMap=new HashMap<>();
        //获取访问域名
        int s=StringUtils.indexOf(domain,".cipherchina.com");
        domain=StringUtils.substring(domain,0,s+16);

        // String domain="sads";
        //如果域名为空 直接返回123456默认companyUuid
        if (StringUtils.isEmpty(domain)){
            resMap.put("return_code", "0");
            resMap.put("companyUUid","123456");
            return resMap;
        }

        String companyUUid=companyService.getCompanyUUid(domain);
        if (StringUtils.isEmpty(companyUUid)){
            resMap.put("return_code", "0");
            resMap.put("companyUUid","123456");
            return resMap;
        }
        resMap.put("return_code", "0");
        resMap.put("companyUUid",companyUUid);
        return resMap;
    }
}
