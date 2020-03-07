package com.portal.controller;


import com.google.gson.Gson;
import com.portal.domain.*;
import com.portal.enums.ResultCode;
import com.portal.service.IdentityModesService;
import com.portal.service.PortalService;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

import static com.portal.enums.ResultCode.PARAMETER_FAILURE;

/**
 * @Author: TK
 * @Date: 2019/4/25 19:30
 */
@Controller
@RequestMapping(value = "/cipher/index")
public class IndexController extends  BaseController{

    private static final Logger LOOGER= LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private IdentityModesService identityModesService;

    @Autowired
    private PortalService portalService;

    /**
     * 根据公司UUid获取protal的配置
     * @param companyUUid
     * @return
     */
    @RequestMapping(value = "/service")
    @ResponseBody
    public Map<String,Object> portalConfigService(String companyUUid){
        Map<String,Object> map=new HashMap<>();

        //入参校验
        if (StringUtils.isEmpty(companyUUid)){
            LOOGER.warn(" enter IndexController.portalConfigService  Error As The companyUUid =[{}]",new Object[]{companyUUid});
            sendBaseErrorMap(PARAMETER_FAILURE);
        }

        //获取身份认证设置
        IdentitySettingInfoDomain identitySettingInfoDomain=identityModesService.getIdentityModesByCompanyUUid(companyUUid);

        //将字符串转换为实体类获取身份认证方式
        IdentityAuthentication identityAuthentication =null;

        //将字符串转换为实体类获取账号注册
        AccountRegistration accountRegistration = null;

        //将字符串转换为实体类获取忘记密码
        ForgetPassword forgetPassword = null;

        //身份设置为空直接返回
        if (identitySettingInfoDomain ==null){
            return sendBaseErrorMap(ResultCode.IDENTITY_NULL);
        }
        if(StringUtils.isNotEmpty(identitySettingInfoDomain.getRegistFlow())){
            accountRegistration = new Gson().fromJson(identitySettingInfoDomain.getRegistFlow(), AccountRegistration.class);
        }

        if ( !identitySettingInfoDomain.getAuthMode().isEmpty()) {
            JSONObject jsonObject = JSONObject.fromObject(identitySettingInfoDomain.getAuthMode());
            identityAuthentication = (IdentityAuthentication) JSONObject.toBean(jsonObject, IdentityAuthentication.class);
        }
        if(StringUtils.isNotEmpty(identitySettingInfoDomain.getForgetFlow())){
            forgetPassword = new Gson().fromJson(identitySettingInfoDomain.getForgetFlow(), ForgetPassword.class);
        }
        //获取登录页的配置信息
        PortalConfigDomain portalConfigDomain= identityModesService.getPortalConfig(companyUUid);

        //版权文案
        String copyright = identityModesService.obtainCopyright(companyUUid);

        //页面图标和标题
        IconSettingsDomain iconSettingsByCompanyUuid = identityModesService.getIconSettingsByCompanyUuid(companyUUid, 2);

        //portal首页的图标信息
        IconSettingsDomain iconSettingsDomain = portalService.getIconSettiingInfo(companyUUid);

        //添加到map中返回
        map.put("iconSettingsDomain",iconSettingsDomain);
        map.put("modes",identityAuthentication);
        map.put("regist", accountRegistration.getIsOpenRegister());
        map.put("forget",forgetPassword.getIsOpenForgetPwd());
        map.put("portalConfig",portalConfigDomain);
        map.put("copyright",copyright);
        map.put("icon",iconSettingsByCompanyUuid);

        return sendBaseNormalMap(map);
    }

}
