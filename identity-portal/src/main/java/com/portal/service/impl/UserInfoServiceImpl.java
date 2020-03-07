package com.portal.service.impl;


import com.portal.commons.ConstantsCMP;
import com.portal.commons.GlobalReturnCode;
import com.portal.daoAuthoriza.GroupDAO;
import com.portal.daoAuthoriza.UserDAO;
import com.portal.domain.*;
import com.portal.enums.UserStateEnum;
import com.portal.model.DabbyUserInfoModel;
import com.portal.publistener.UserBehaviorPublistener;
import com.portal.service.AuthUserService;
import com.portal.service.GroupService;
import com.portal.service.LoginFailedUserService;
import com.portal.service.UserInfoService;
import com.portal.utils.IpUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 95744 on 2018/6/29.
 */

@Service
public class UserInfoServiceImpl  implements UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);


    @Autowired
    private UserDAO userDAO;

   /* @Autowired
    private EmailService emailService;
*/
    @Autowired
    private AuthUserService authUserService;

    @Autowired
    private LoginFailedUserService loginFailedUserService;


    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private GroupService groupService;



    @Override
    public UserInfoDomain queryUserInfo(UserInfoDomain userInfoDomain) {
        return userDAO.selectUserInfo(userInfoDomain);
    }

    @Override
    public void updateUserPwd(UserInfoDomain UserInfoDomain) {
        userDAO.updateUserPwd(UserInfoDomain);

    }

    @Override
    public void updateUserInfo(UserInfoDomain userInfoDomain) {
      /*  if(!StringUtils.isEmpty(userInfoDomain.getGroupId())){
            userDAO.updateGroupId(userInfoDomain);
        }*/
        userDAO.updateUserInfo(userInfoDomain);
    }

    @Override
    public List<GroupInfoDomain> queryGroupSelect(String groupId) {
        return userDAO.queryGroupSelect(groupId);
    }

    @Override
    public String getFirstPwd() {
        return userDAO.getFirstPwd();
    }

    @Override
    public String getPwd(String userId) {
        return userDAO.getPwd(userId);
    }

    @Override
    public SubClientInfoDomain getSubNameInfo(String uuid, String clientId) {
        logger.info("username:"+uuid +"   "+ "clientId:"+clientId);
        SubClientInfoDomain subClientInfoDomain = userDAO.getSubNameInfo(uuid,clientId);
        if (subClientInfoDomain != null){
            subClientInfoDomain.passwordDecrypt();
        }
        return subClientInfoDomain;
    }

    @Override
    public int insertSubName(SubClientInfoDomain subClientInfoDomain) {
             subClientInfoDomain.passwordEncrypt();
             SubClientInfoDomain newdomain = userDAO.getSubName(subClientInfoDomain);
             SubAccountMapDomain domain = new SubAccountMapDomain();
             if (null == newdomain) {
                 int subId = userDAO.insertSubName(subClientInfoDomain);
                 domain.setSubId(subClientInfoDomain.getSubId());
                 domain.setAccountNumber(subClientInfoDomain.getAccountNumber());
                 return userDAO.insertSubNameMap(domain);
             } else {
                 domain.setAccountNumber(subClientInfoDomain.getAccountNumber());
                 domain.setSubId(newdomain.getSubId());
                 SubAccountMapDomain mapdomain = userDAO.getSubAccountMap(domain);
                 if (null == mapdomain) {
                     userDAO.insertSubNameMap(domain);
                 }
                 newdomain.passwordDecrypt();
                 domain.setSubId(newdomain.getSubId());
                 domain.setAccountNumber(subClientInfoDomain.getAccountNumber());
                 return userDAO.updateSubName(subClientInfoDomain);
             }
         }

    @Override
    public void insertUserInfo(DabbyUserInfoModel userInfoModel) {
        userDAO.insertUserInfo(userInfoModel);
        groupDAO.insertUserGroupMap(new UserGroupMap(userInfoModel.getId_num()));
    }


    @Override
    public UserInfoDomain getUserInfo(String uuid,String companyUuid) {
        return userDAO.getUserInfo(uuid,companyUuid);
    }

    @Override
    public Map<String, Object> sendkapataMsg(String email, int type,String username) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (type == ConstantsCMP.SendMsgConstant.EMAIL) {
            //map = emailService.getSendMsg(email,username);
        }
        return map;
    }

    @Override
    public Map<String, Object> getkapataMsg(String email, int type,String kapataCode,String username) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (type == ConstantsCMP.SendMsgConstant.EMAIL) {
          //  map = emailService.getKapata(email,kapataCode,username);
        }
        return map;
    }

    @Override
    public UserInfoDomain getUserInfoByConditon(String accountNumber, String pwd) {
        return userDAO.getUserInfoByconditon(accountNumber,pwd);
    }

    @Override
    public GlobalReturnCode.MsgCodeEnum checkUserInvalid(String userName) throws Exception {

            UserInfoDomain userInfoDomain = authUserService.queryUserByName(userName);

            //账号验证从错误次数太多，被冻结
            if (loginFailedUserService.isUserFreezed(userName)) {
                return GlobalReturnCode.MsgCodeEnum.USER_ARE_FREEZED;
            }

            if ("停用".equals(userInfoDomain.getAccountStatus())) {
                return GlobalReturnCode.MsgCodeEnum.ACCOUNT_STATUS_IVALIDE;
            }

            return GlobalReturnCode.MsgCodeEnum.AUTHUSER_SUCCESS;

        }

    @Override
    public List<TeamInfo> getTeamList(String accountNumber) {
        return userDAO.getTeamList(accountNumber);
}

    @Override
    public UserInfoDomain getUserInfoByMail(String mail) {
        //入参校验
        if (StringUtils.isBlank(mail)){
            return null;
        }

        return userDAO.getUserInfoByMail(mail);
    }

    @Override
    public UserInfoDomain getUserInfoByPhone(String phoneNumber) {
        //入参校验
        if (StringUtils.isBlank(phoneNumber)){
            return null;
        }
        return userDAO.getUserInfoByPhone(phoneNumber);
    }

    @Override
    public UserInfoDomain getUserBySaifu(String userName,String companyUUid) {
        //入参校验
        if (StringUtils.isBlank(userName)){
            return null;
        }
        return userDAO.getUserBySaifu(userName,companyUUid);
    }

    @Override
    public UserInfoDomain getUserByDabai(String userName,String companyUUid) {
        //入参校验
        if (StringUtils.isBlank(userName)){
            return null;
        }
        return userDAO.getUserByDabai(userName,companyUUid);
    }

    @Override
    public UserInfoDomain getUserInfoByDingding(String userName,String companyUUid) {
        //入参校验
        if (StringUtils.isBlank(userName)){
            return null;
        }
        return userDAO.getUserByDingding(userName,companyUUid);
    }

    @Override
    public UserInfoDomain getUserByDingTalkByUnionIdAndCompanyId(String unionId,String companyUUid) {
        return userDAO.getUserByDingTalkByUnionIdAndCompanyId(unionId, companyUUid);
    }

    @Override
    public boolean registUser(RegisterApprovalDomain registerApprovalDomain) {
        try {
            userDAO.registUser(registerApprovalDomain);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public RegisterApprovalDomain getRegistUser(RegisterApprovalDomain registerApprovalDomain) {
        //入参校验
        if (registerApprovalDomain==null){
            return null;
        }
        return userDAO.getRegistUser(registerApprovalDomain);
    }

    @Override
    public UserInfoDomain getUser(String userPlantId ,String companyUUid) {

        //入参校验
        if (StringUtils.isBlank(userPlantId)){
            return null;
        }
        return userDAO.getUser(userPlantId ,companyUUid);
    }

    @Override
    public UserInfoDomain getUserByMailAndPhone(String phoneNumber, String mail) {

        if (StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(mail)){
            return null;
        }
        return userDAO.getUserByMailAndPhone(phoneNumber,mail);
    }

    @Override
    public UserInfoDomain getUserByMailOrPhone(String phoneNumber, String mail) {
        //入参校验
        if (StringUtils.isBlank(phoneNumber) || StringUtils.isBlank(mail)){
            return null;
        }
        return userDAO.getUserByMailOrPhone(phoneNumber,mail);
    }

    @Override
    public UserInfoDomain getUserInfoByUUid(String uuid) {
        try {
            UserInfoDomain userInfoDomain = userDAO.getUserInfoByUUid(uuid);
            return userInfoDomain;
        }catch (Exception e){
            return null;
        }
    }

    @Override
    public boolean updateUserPhone(UserInfoDomain userInfo) {
        try {
            userDAO.updateUserPhone(userInfo);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public boolean updateUserPwdByUuid(UserInfoDomain userInfo) {
        try {
            userDAO.updateUserPwdByUuid(userInfo);
            return true;
        }catch (Exception e){
            return false;
        }
    }

    @Override
    public UserInfoDomain selectUserInfoByDingTalkInfo(String companyId, String unionId) {
        return userDAO.selectUserInfoByDingTalkInfo(companyId, unionId);
    }

    @Override
    public UserInfoDomain selectUserInfoByIdNum(String idNum) {
        return userDAO.selectUserInfoByIdNum(idNum);
    }

    @Override
    public UserInfoDomain getCompanyUserInfoByPhoneNumber(String phoneNumber, String companyId) {
        return userDAO.getCompanyUserInfoByPhoneNumber(phoneNumber,companyId);
    }

    @Override
    public UserInfoDomain getCompanyUserInfoByMail(String mail, String companyId) {
        return userDAO.getCompanyUserInfoByMail(mail, companyId);
    }

    @Override
    public UserStateEnum getUserState(String uuid) {
        UserInfoDomain userInfo = userDAO.getUserInfoByUUid(uuid);

        return getUserState(userInfo);
    }

    @Override
    public UserStateEnum getUserState(UserInfoDomain userInfo) {
        //用户不存在
        if (userInfo == null){
            return UserStateEnum.IS_EXIT;
        }
        //用户不可用
        if (!userInfo.isAvailable()){
            return UserStateEnum.IS_STOP;
        }
        //用户锁定
        if(loginFailedUserService.isUserFreezed(userInfo.getUuid())){
            return UserStateEnum.IS_LOCK;
        }
        return null;
    }

    @Override
    public boolean updateUserMail(UserInfoDomain userInfo) {
        try{
            userDAO.updateUserMail(userInfo);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public UserInfoDomain getUserInfoAndGroupsByUUid(String uuid) {
        //入参校验
        if (org.apache.commons.lang.StringUtils.isEmpty(uuid)){
            logger.error("enter UserInfoServiceImpl.getUserInfoAndGroupsByUUid(String uuid) error ,params uuid is null",uuid);
            return null;
        }
        //获取用户信息
        UserInfoDomain userInfoDomain=getUserInfoByUUid(uuid);
        //根据uuid 获取部门的字符串拼接
        String groups=groupService.getGroupsStringByUuid(uuid);
        //将groups 设置userInfoDomain
        userInfoDomain.setGroupName(groups);
        return userInfoDomain;
    }


    @Override
    public UserInfoDomain selectUserInfoByWeixinInfo(String userName, String companyUUid) {
        return userDAO.selectUserInfoByWeixinInfo(userName,companyUUid);
    }


}

