package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.NewReturnUtils;
import cipher.console.oidc.domain.web.IconSettingsDomain;
import cipher.console.oidc.mapper.IconSettingsMapper;
import cipher.console.oidc.service.ObtainCopyrightService;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.util.HttpRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 对公司版权文案进行编辑
 * @author cozi
 * @date 2019-08-05
 */
@Controller
@RequestMapping("/cipher/obtain")
public class ObtainCopyrightController {

    @Autowired
    private ObtainCopyrightService obtainCopyrightService;

    @Autowired
    private IconSettingsMapper iconSettingsMapper;

    @Autowired
    private SessionService sessionService;

    @RequestMapping("/copyright")
    @ResponseBody
    public Map<String,Object> getCopyright(HttpServletRequest request,@RequestParam(value = "companyUUid") String companyUUid){
        //String companyUUid = sessionService.getCompanyUUid(request.getSession());
        Map<String,Object> map = new HashMap<>();
        String copyright = obtainCopyrightService.getCopyright(companyUUid);
        IconSettingsDomain iconSettingsDomain = iconSettingsMapper.selectIconSettingsByCompanyUuid(companyUUid, 4);
        map = NewReturnUtils.successResponse("公司版权文案获取成功");
        map.put("copyright", copyright);
        map.put("icon",iconSettingsDomain);
        return map;
    }

}
