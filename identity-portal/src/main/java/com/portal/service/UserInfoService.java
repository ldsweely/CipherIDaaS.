package com.portal.service;



import com.portal.commons.GlobalReturnCode;
import com.portal.domain.*;
import com.portal.enums.UserStateEnum;
import com.portal.model.DabbyUserInfoModel;

import java.util.List;
import java.util.Map;



public interface UserInfoService {

    public UserInfoDomain queryUserInfo(UserInfoDomain userInfoDomain);

    void updateUserPwd(UserInfoDomain UserInfoDomain);

    void updateUserInfo(UserInfoDomain userInfoDomain);

    public List<GroupInfoDomain> queryGroupSelect(String groupId);


    String getFirstPwd();

    String getPwd(String accoutNumber);


    SubClientInfoDomain getSubNameInfo(String username, String clientId);

    int insertSubName(SubClientInfoDomain subClientInfoDomain);


    public UserInfoDomain getUserInfo(String accountNumber,String companyUuid);

    public Map<String,Object> sendkapataMsg(String email, int type,String username);

    public Map<String,Object> getkapataMsg(String email,int type,String kapataCode,String username);


    public UserInfoDomain getUserInfoByConditon(String accountNumber,String pwd);



    public GlobalReturnCode.MsgCodeEnum checkUserInvalid(String userName) throws Exception;

    void insertUserInfo(DabbyUserInfoModel userInfoModel);



    public List<TeamInfo> getTeamList(String accountNumber);


    UserInfoDomain getUserInfoByMail(String mail);

    UserInfoDomain getUserInfoByPhone(String phoneNumber);

    UserInfoDomain getUserBySaifu(String userName,String companyUUid);

    UserInfoDomain getUserByDabai(String userName,String companyUUid);

    UserInfoDomain getUserInfoByDingding(String userName,String companyUUid);

    UserInfoDomain  getUserByDingTalkByUnionIdAndCompanyId(String unionId,String companyId);

    boolean registUser(RegisterApprovalDomain registerApprovalDomain);

    RegisterApprovalDomain getRegistUser(RegisterApprovalDomain registerApprovalDomain);

    UserInfoDomain getUser(String userPlantId ,String companyUUid);

    UserInfoDomain getUserByMailAndPhone(String phoneNumber, String mail);

    UserInfoDomain getUserByMailOrPhone(String phoneNumber, String mail);

    UserInfoDomain getUserInfoByUUid(String uuid);

    boolean updateUserPhone(UserInfoDomain userInfo);

    boolean  updateUserPwdByUuid(UserInfoDomain userInfo);

    UserInfoDomain selectUserInfoByDingTalkInfo(String companyId,String unionId);

    UserInfoDomain selectUserInfoByIdNum(String idNum);

    UserInfoDomain getCompanyUserInfoByPhoneNumber(String phoneNumber,String companyId);

    UserInfoDomain getCompanyUserInfoByMail(String mail, String companyId);

    public UserStateEnum getUserState(String uuid);

    UserStateEnum getUserState(UserInfoDomain userInfo);

    boolean updateUserMail(UserInfoDomain userInfo);

    /**
     * create by 安歌
     * create time  2019年7月25日14:58:39
     * 根据用户的uuid获取用户的信息  并且带上部门字符串的拼接
     * @param uuid
     * @return
     */
    UserInfoDomain getUserInfoAndGroupsByUUid(String uuid);


    UserInfoDomain  selectUserInfoByWeixinInfo(String userName,String companyUUid);
}
