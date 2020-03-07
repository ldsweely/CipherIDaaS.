package com.portal.service;


import com.dingtalk.api.response.OapiGettokenResponse;
import com.dingtalk.api.response.OapiUserGetResponse;
import com.dingtalk.api.response.OapiUserGetUseridByUnionidResponse;
import com.portal.domain.*;


/**
 * @Author: TK
 * @Date: 2019/5/8 15:37
 */
public interface BindDingServiceInfo {
    DingdingConfigInfo getDingInfoByCompanyUUid(String companyUUid);

    OapiGettokenResponse getDingDingAccessToken(DingdingConfigInfo dingdingConfigInfo);

    OapiUserGetUseridByUnionidResponse getDingDingUserId(String dingDingId , String accessToken);

    OapiUserGetResponse getDingDingUserInfo(String accessToken, String userId);

    UserInfoDomain bindLocalAccount(String phoneNumber, String companyId, String mail, int rule);

    BindingDingInfoDomain getDingDingInfoDomain(String uuid);

    boolean insertBindDing(BindingDingInfoDomain dingInfoDomain);

    UserInfoDomain getUserByAccountNumber(String daiBaiId,String companyUuid);

    DaBaiBindingInfoDomain getDaBaiBindingInfo(String uuid);

    boolean insertDaBaiBinding(DaBaiBindingInfoDomain daBaiBindingInfoDomain);

    UserInfoDomain getUserByPhoneNumber(String phoneNumber);

    SaiFuBindingInfoDomain getSaiFuBindingInfo(String uuid);

    boolean insertBindDingSaiFu(SaiFuBindingInfoDomain saiFuBindingInfoDomain1);

    DaBaiBindingInfoDomain getDabaiBindingInfoByIdNum(String idNum,String companyId);

    SaiFuBindingInfoDomain getSaiFuBindingInfoBySaiFuId(String platId,String companyId);

    BindingDingInfoDomain getDingTalkInfoByUnionId(String unionId,String companyId);

    BindWxInfoDomain getWxInfoByUnionId(String wxUserId,String companyId);

    boolean insertWeiXinInfo(BindWxInfoDomain bindWxInfoDomain);
}
