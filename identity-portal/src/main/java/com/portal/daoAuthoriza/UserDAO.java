package com.portal.daoAuthoriza;


import com.portal.domain.*;
import com.portal.model.DabbyUserInfoModel;
import com.portal.model.UserAgentModel;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDAO {
    List<UserInfoDomain> selectAllUser();

    UserInfoDomain selectUserByUsername(String username);

    void updateUserPwd(UserInfoDomain userInfoDomain);

    UserInfoDomain selectUserInfo(UserInfoDomain userInfoDomain);


    void updateUserInfo(UserInfoDomain userInfoDomain);


    public List<GroupInfoDomain> queryGroupSelect(@Param("groupId") String groupId);


    String getFirstPwd();

    public String getPwd(@Param("userId") String userId);


    void updateGroupId(UserInfoDomain userInfoDomain);

    public String getLength();

    public List<Integer> getGuize();

    public SubClientInfoDomain getSubNameInfo(@Param("uuid") String uuid, @Param("clientId") String clientId);

    public int insertSubName(SubClientInfoDomain subClientInfoDomain);

    public int insertSubNameMap(SubAccountMapDomain subAccountMapDomain);

    public SubClientInfoDomain getSubName(SubClientInfoDomain subClientInfoDomain);


    public int updateSubName(SubClientInfoDomain subClientInfoDomain);

    public UserInfoDomain getUserInfo(@Param("uuid") String uuid,@Param("companyUuid") String companyUuid);

    public  SubAccountMapDomain getSubAccountMap(SubAccountMapDomain subAccountMapDomain);

    public void insertUserInfo(DabbyUserInfoModel userInfoModel);

    UserInfoDomain selectUser(@Param("uuid")String uuid,@Param("companyUuid")String companyUuid);

    public UserInfoDomain getUserInfoByconditon(@Param("accountNumber")String accountNumber,@Param("password")String pwd);

    public List<TeamInfo> getTeamList(@Param("userId")String userId);

    void insertUserAgentInfo(UserAgentModel userAgentModel);

    void insertPwd(UserInfoDomain userInfoDomain);

    Integer getPwdType();


    UserInfoDomain getUserInfoByMail(String mail);

    UserInfoDomain getUserInfoByPhone(String phoneNumber);

    UserInfoDomain getUserBySaifu(@Param("userName")String userName, @Param("companyId") String companyId);

    UserInfoDomain getUserByDabai(@Param("userName")String userName, @Param("companyId")String companyId);

    UserInfoDomain getUserByDingding(@Param("userName")String userName, @Param("companyUUid")String companyUUid);

    int getPwdTypeByCompanyUuid(String companyUUid);

    RegisterApprovalDomain getRegistUser(RegisterApprovalDomain registerApprovalDomain);

    void registUser(RegisterApprovalDomain registerApprovalDomain) throws Exception;

    UserInfoDomain getUser(@Param("userPlantId")String userPlantId, @Param("companyUUid")String companyUUid);

    UserInfoDomain getUserByMailAndPhone(@Param("phoneNumber")String phoneNumber, @Param("mail")String mail);

    UserInfoDomain getUserByMailOrPhone(@Param("phoneNumber") String phoneNumber, @Param("mail") String mail);

    UserInfoDomain selectUserInfoWithPassword(String userName);

    UserInfoDomain selectUserInfoWithPlatId(String userId);

    UserInfoDomain getUserInfoByUUid(@Param("uuid")String uuid);

    void updateUserPhone(UserInfoDomain userInfo);

    void updateUserPwdByUuid(UserInfoDomain userInfo);

    UserInfoDomain selectUserInfoByDingTalkInfo(@Param("companyId")String companyId,
                                                @Param("dingUnionId")String dingUnionId);

    UserInfoDomain selectUserInfoByIdNum(@Param("idNum")String idNum);

    UserInfoDomain getCompanyUserInfoByPhoneNumber(@Param("phoneNumber")String phoneNumber,
                                                      @Param("companyId")String companyId);

    UserInfoDomain getCompanyUserInfoByMail(@Param("mail")String mail, @Param("companyId")String companyId);

    UserInfoDomain selectUserInfoWithPasswordByUserId(String userId);

    Integer getPasswordTypeByCompanyId(String companyId);

    UserInfoDomain  getUserByDingTalkByUnionIdAndCompanyId(@Param("unionId")String unionId,
                                                           @Param("companyId")String companyId);

    UserInfoDomain getUserCompanyByMailAndPhone(@Param("companyId")String companyId,
                                                @Param("phoneNumber")String phoneNumber,
                                                @Param("mail")String mail);

    UserInfoDomain getUserCompanyByMailOrPhone(@Param("companyId")String companyId,
                                                @Param("phoneNumber")String phoneNumber,
                                                @Param("mail")String mail);

    List<String> getUserGroupIdListByUuid(@Param("uuid") String uuid,@Param("companyId") String companyId);

    List<String> getTeamListByUuidAndCompanyId(@Param("uuid") String uuid,@Param("appId") String appId);

    UserInfoDomain selectUserInfoWithAdInfo(@Param("companyId")String companyId,
                                            @Param("userName")String userName);

    void updateUserMail(UserInfoDomain userInfo);


    UserInfoDomain selectUserInfoByWeixinInfo(@Param("wxUserId")String wxUserId,
                                            @Param("companyId")String companyId);




}
