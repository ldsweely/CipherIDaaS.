package com.portal.controller;


import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.google.gson.Gson;
import com.portal.bto.PortalAppBto;
import com.portal.commons.Constants;
import com.portal.commons.ConstantsCMP;
import com.portal.commons.ReturnCode;
import com.portal.domain.*;
import com.portal.model.GroupApplicationModel;
import com.portal.service.*;
import com.portal.token.CheckToken;
import com.portal.utils.ConstantsUrls;
import com.portal.utils.IpUtil;
import com.portal.utils.aes.AES;
import com.portal.utils.aes.AesKey;
import com.portal.utils.rsa.HengyiRsaKey;
import com.portal.utils.rsa.RSATool;
import com.portal.utils.rsa.RSAToolNew;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.portal.enums.ResultCode.*;

@RestController
@RequestMapping(value = "/cipher/portal")
public class PortalController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(PortalController.class);

    @Autowired
    private PortalService portalService;


    @Autowired
    private UserBehaviorService userBehaviorService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private ObjectFactoryService objectFactoryService;


    /*
     * portal首页
     * (数据隔离修改)
     * */
    @CheckToken
    @RequestMapping(value = "/index", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> index(HttpServletRequest request,
                                     @RequestParam(value = "companyUUid") String companyUuid,
                                     @RequestParam(value = "userId") String uuid) {

        Map<String, Object> map = new HashMap<>();
        try {
            if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(companyUuid)) {
                map.put("return_code", ReturnCode.FAIL);
                map.put("return_msg", "参数错误");
                return map;
            }
            UserInfoDomain domain = userInfoService.getUserInfoByUUid(uuid);
            if (null == domain) {
                map.put("return_code", ReturnCode.FAIL);
                map.put("return_msg", "用户不存在");
                return map;
            }
            GroupInfoDomain groupInfoDomain = new GroupInfoDomain();
            groupInfoDomain.setUserId(uuid);
            groupInfoDomain.setCompanyId(companyUuid);
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo();
            userBehaviorInfo.setUserId(uuid);
            UserBehaviorInfo user = userBehaviorService.selectUserBehaviorInfo(userBehaviorInfo);
            if (null != user && StringUtils.isNotEmpty(user.getCreateTime().toString())) {
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.getCreateTime());
                map.put("loginTime", dateStr);
                map.put("ip", user.getIp());
            } else {
                map.put("loginTime", "");
                map.put("ip", "");
            }
            IconSettingsDomain iconSettingsDomain = portalService.getIconSettiingInfo(companyUuid);
            map.put("IconSettingsDomain", iconSettingsDomain);
            map.put("isAdmin", domain.getIsAdmin());
            map.put("username", domain.getUserName());
            map.put("return_msg", "返回数据成功");
            map.put("return_code", ReturnCode.SUCCESS);
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            map.put("return_code", ReturnCode.FAIL);
            map.put("return_msg", "服务器错误");
        }
        return map;
    }


    /*
     * 获取应用列表
     *
     * */
    @CheckToken
    @RequestMapping(value = "/getAppList", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getAppList(HttpServletRequest request,
                                          @RequestParam(value = "companyUUid") String companyUuid,
                                          @RequestParam(value = "userId") String uuid,String queryName) {

        Map<String, Object> map = new HashMap<>();
        try {
            if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(companyUuid)) {
                map.put("return_code", ReturnCode.FAIL);
                map.put("return_msg", "参数错误");
                return map;
            }
            GroupInfoDomain groupInfoDomain = new GroupInfoDomain();
            groupInfoDomain.setUserId(uuid);
            groupInfoDomain.setCompanyId(companyUuid);
            GroupInfoDomain groupInfo = groupService.getGroupInfoInfo(groupInfoDomain);
            /*if (null == groupInfo) {
                map.put("return_code", ReturnCode.FAIL);
                map.put("return_msg", "用户部门不存在");
                return map;
            }*/
            UserInfoDomain domain = userInfoService.getUserInfoByUUid(uuid);
            if (null == domain) {
                map.put("return_code", ReturnCode.FAIL);
                map.put("return_msg", "用户不存在");
                return map;
            }
            List<GroupApplicationModel> GroupApplicationList = new ArrayList<>();
            if (null != groupInfo || groupInfo.getGroupId().equals("0")) {
                //获取所有应用集合
                GroupApplicationList = portalService.selectNewApplicationList(uuid,queryName);
            }
            UserBehaviorInfo userBehaviorInfo = new UserBehaviorInfo();
            userBehaviorInfo.setUserId(uuid);
            UserBehaviorInfo user = userBehaviorService.selectUserBehaviorInfo(userBehaviorInfo);
            if (null != user && StringUtils.isNotEmpty(user.getCreateTime().toString())) {
                String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(user.getCreateTime());
                map.put("loginTime", dateStr);
                map.put("ip", user.getIp());
            } else {
                map.put("loginTime", "");
                map.put("ip", "");
            }
            map.put("return_code", ReturnCode.SUCCESS);
            map.put("return_msg", "获取应用信息成功");
            map.put("portalList", GroupApplicationList);
            map.put("isAdmin", domain.getIsAdmin());
        } catch (Exception e) {
            logger.debug(e.getMessage(), e);
            map.put("return_code", ReturnCode.FAIL);
            map.put("return_msg", "应用列表为空");
        }
        return map;
    }




    /*
     * 获取应用权限列表
     * @param userId
     * @param companyUUid
     * */

    @RequestMapping(value = "/getUserAppList", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> getUserAppList(HttpServletRequest request,
                                              @RequestParam(value = "companyUUid", required = false, defaultValue = "123456") String companyUuid,
                                              @RequestParam(value = "userId") String uuid,
                                              @RequestParam(value = "token") String token,
                                              @RequestParam(value = "timestamp") String timestamp) throws  UnsupportedEncodingException {

        Map<String, Object> map = new HashMap<>();
        logger.info("userId----"+uuid+"----token-----"+token+"-----time------"+timestamp);
        if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(token)|| StringUtils.isEmpty(timestamp)) {

            return sendBaseErrorMap(PARAMETER_FAILURE);
        }

       /* boolean check = RSATool.verifySHA1withRSASigature(token, uuid+timestamp, RSAToolNew.PRI_KEY);
        if (!check) {
            sendBaseErrorMap(SIGN_ERROR);
        }*/

        boolean check= false;
        try {
            check = AlipaySignature.rsa256CheckContent(uuid + timestamp, URLDecoder.decode(token, "UTF-8"), HengyiRsaKey.PUB_KEY, "utf-8");
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return sendBaseErrorMap(SIGN_ERROR);
        }

        if (!check) {
           return sendBaseErrorMap(SIGN_ERROR);
        }

        GroupInfoDomain groupInfoDomain = new GroupInfoDomain();
        groupInfoDomain.setUserId(uuid);
        groupInfoDomain.setCompanyId(companyUuid);
        GroupInfoDomain groupInfo = groupService.getGroupInfoInfo(groupInfoDomain);
        if (null == groupInfo) {
            return sendBaseErrorMap(USER_IS_NOT_EXIST);
        }
        UserInfoDomain domain = userInfoService.getUserInfoByUUid(uuid);
        if (null == domain) {
            return  sendBaseErrorMap(USER_IS_NOT_EXIST);
        }
        //获取所有应用集合
        List<GroupApplicationModel> GroupApplicationList = portalService.selectNewApplicationList(uuid,"");
        if (null == GroupApplicationList || GroupApplicationList.size() < 0) {
            return sendBaseErrorMap(APPLICATION_NO_EXIT);
        }

        List<PortalAppBto> portalAppBtoList = objectFactoryService.object2tBrandList(GroupApplicationList);
        String app = JSON.toJSONString(portalAppBtoList);
        map.put("return_code", ReturnCode.SUCCESS);
        map.put("appList", app);
        return map;
    }




    @RequestMapping(value = "/test")
    public void getTest(HttpServletRequest request){
        HttpSession session = request.getSession();
        System.out.println("jsessionId:"+session.getId());

    }






}

