package com.portal.service.impl;



import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiGettokenRequest;
import com.dingtalk.api.request.OapiUserGetRequest;
import com.dingtalk.api.request.OapiUserGetUseridByUnionidRequest;
import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiUserGetResponse;
import com.dingtalk.api.response.OapiUserGetUseridByUnionidResponse;
import com.portal.daoAuthoriza.BindDingServiceInfoMapper;
import com.portal.daoAuthoriza.UserDAO;
import com.portal.domain.*;
import com.portal.service.BindDingServiceInfo;
import com.taobao.api.ApiException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: TK
 * @Date: 2019/5/8 15:38
 */
@Service
public class BindDingServiceInfoImpl implements BindDingServiceInfo {

    @Autowired
    private UserDAO userDAO;
    @Autowired
    private BindDingServiceInfoMapper bindDingServiceInfoMapper;
    @Override
    public DingdingConfigInfo getDingInfoByCompanyUUid(String companyUUid) {

        //入参校验
        if (StringUtils.isBlank(companyUUid)){
            return null;
        }
        return bindDingServiceInfoMapper.getDingInfoByCompanyUUid(companyUUid);
    }

    @Override
    public OapiGettokenResponse getDingDingAccessToken(DingdingConfigInfo dingdingConfigInfo) {

        //入参校验
        if (dingdingConfigInfo==null){
            return null;
        }
        //打开钉钉的连接
        DefaultDingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/gettoken");

        //创建传输对象
        OapiGettokenRequest request = new OapiGettokenRequest();

        //设置请求的appkey
        request.setAppkey(dingdingConfigInfo.getAppKey());

        //设置钉钉的appsecret
        request.setAppsecret(dingdingConfigInfo.getAppSecret());

        //设置请求
        request.setHttpMethod("GET");
        OapiGettokenResponse response =null;
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public OapiUserGetUseridByUnionidResponse getDingDingUserId(String dingDingId,String accessToken) {
        //入参校验
        if (StringUtils.isBlank(dingDingId) || StringUtils.isBlank(accessToken)){
            return null;
        }
        //打开钉钉的连接
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/getUseridByUnionid");


        //创建传输对象
        OapiUserGetUseridByUnionidRequest request = new OapiUserGetUseridByUnionidRequest();

        //设置请求的appkey
        request.setUnionid(dingDingId);
        request.setHttpMethod("GET");

        //创建接收对象
        OapiUserGetUseridByUnionidResponse response =null;
        try {
            response = client.execute(request, accessToken);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return response;
    }

    @Override
    public OapiUserGetResponse getDingDingUserInfo(String accessToken, String userId) {
        //入参校验
        if (StringUtils.isBlank(accessToken) || StringUtils.isBlank(userId)){
            return null;
        }

        //创建钉钉客户端连接对象
        DingTalkClient client = new DefaultDingTalkClient("https://oapi.dingtalk.com/user/get");

        //创建请求并设置userid的值
        OapiUserGetRequest request = new OapiUserGetRequest();
        request.setUserid(userId);
        request.setHttpMethod("GET");

        OapiUserGetResponse response=null;
        try {
            //发起get请求
            response = client.execute(request, accessToken);
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return response;
    }

    @Override
    public UserInfoDomain bindLocalAccount(String phoneNumber, String companyId, String mail, int rule) {

        UserInfoDomain userInfoDomain =null;

        boolean isMailEmpty = StringUtils.isEmpty(mail);

        boolean isPhoneEmpty = StringUtils.isEmpty(phoneNumber);

        if (isMailEmpty && isPhoneEmpty){
            return null;
        }

        //如果rule为手机号匹配
        if (rule==DingdingConfigInfo.PHONE_NUMBER && !isPhoneEmpty){
            userInfoDomain= userDAO.getCompanyUserInfoByPhoneNumber(phoneNumber, companyId);
            return userInfoDomain;
        }

        //如果以邮箱匹配
        if (rule==DingdingConfigInfo.MAIL && !isMailEmpty){
            userInfoDomain = userDAO.getCompanyUserInfoByMail(mail, companyId);
            return userInfoDomain;
        }

        //如果是手机号和邮箱
        if (rule == DingdingConfigInfo.PHONE_AND_MAIL && !isMailEmpty && !isPhoneEmpty){
            userInfoDomain= userDAO.getUserCompanyByMailAndPhone(companyId, phoneNumber, mail);
            return userInfoDomain;
        }

        //如果是手机号或者邮箱,且邮箱不为空
        if (rule == DingdingConfigInfo.PHONE_OR_MAIL && !isMailEmpty){
            userInfoDomain = userDAO.getCompanyUserInfoByMail(mail, companyId);
        }

        //如果是手机号或者邮箱,且手机号不为空
        if (rule == DingdingConfigInfo.PHONE_OR_MAIL && !isPhoneEmpty && userInfoDomain == null){
            userInfoDomain = userDAO.getCompanyUserInfoByPhoneNumber(phoneNumber, companyId);
        }

        return userInfoDomain;
    }

    @Override
    public BindingDingInfoDomain getDingDingInfoDomain(String uuid) {
        return bindDingServiceInfoMapper.getDingDingInfoDomain(uuid);
    }

    @Override
    public boolean insertBindDing(BindingDingInfoDomain dingInfoDomain){
        try {
            bindDingServiceInfoMapper.insertBindDing(dingInfoDomain);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public UserInfoDomain getUserByAccountNumber(String daiBaiId,String companyUuid) {

        //入参校验
        if (StringUtils.isBlank(daiBaiId)){
            return null;
        }

        return userDAO.getUserInfo(daiBaiId,companyUuid);
    }

    @Override
    public DaBaiBindingInfoDomain getDaBaiBindingInfo(String uuid) {
        //入参校验
        if (StringUtils.isBlank(uuid)){
            return null;
        }
        return bindDingServiceInfoMapper.getDabaiBindingInfo(uuid);
    }

    @Override
    public boolean insertDaBaiBinding(DaBaiBindingInfoDomain daBaiBindingInfoDomain1){
        try {
            bindDingServiceInfoMapper.insertDaBaiBinding(daBaiBindingInfoDomain1);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public UserInfoDomain getUserByPhoneNumber(String phoneNumber) {
        //入参校验
        if (StringUtils.isBlank(phoneNumber)){
            return null;
        }

        return userDAO.getUserInfoByPhone(phoneNumber);
    }

    @Override
    public SaiFuBindingInfoDomain getSaiFuBindingInfo(String uuid) {

        //入参校验
        if (StringUtils.isBlank(uuid)){
            return null;
        }

        return bindDingServiceInfoMapper.getSaiFuBindingInfo(uuid);
    }

    @Override
    public boolean insertBindDingSaiFu(SaiFuBindingInfoDomain saiFuBindingInfoDomain1) {
        try {
            bindDingServiceInfoMapper.insertBindDingSaiFu(saiFuBindingInfoDomain1);
            return true;
        }catch (Exception e){
            return false;
        }

    }

    @Override
    public DaBaiBindingInfoDomain getDabaiBindingInfoByIdNum(String idNum,String companyId) {
        return bindDingServiceInfoMapper.getDabaiBindingInfoByIdNum(idNum,companyId);
    }

    @Override
    public SaiFuBindingInfoDomain getSaiFuBindingInfoBySaiFuId(String platId, String companyId) {
        return bindDingServiceInfoMapper.getSaiFuBindingInfoBySaiFuId(platId, companyId);
    }

    @Override
    public BindingDingInfoDomain getDingTalkInfoByUnionId(String unionId, String companyId) {
        return bindDingServiceInfoMapper.getDingTalkInfoByUnionId(unionId, companyId);
    }

    @Override
    public BindWxInfoDomain getWxInfoByUnionId(String wxUserId, String companyId) {
        return bindDingServiceInfoMapper.getWxInfoByUnionId(wxUserId,companyId);
    }

    @Override
    public boolean insertWeiXinInfo(BindWxInfoDomain bindWxInfoDomain) {
        return bindDingServiceInfoMapper.insertWeiXinInfo(bindWxInfoDomain);
    }
}
