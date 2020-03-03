package cipher.console.oidc.mapper;

import cipher.console.oidc.domain.web.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RegisterApprovalMapper {
    int countApprovalListBycompanyUuid(String companyUuid);
    List<RegisterApprovalDomain> selectApprovalListBycompanyUuid(RegisterApprovalDomain registerApprovalDomain);

    int countRecordsListBycompanyUuid(String companyUuid);
    List<RegisterApprovalDomain> selectRecordsListBycompanyUuid(RegisterApprovalDomain registerApprovalDomain);


    RegisterApprovalDomain selectRegisterByuuid(@Param(value = "uuid")String uuid);
    //修改注册审批状态
    void ApprovalResult(@Param(value = "companyUuid") String companyUuid,@Param(value = "uuid")String uuid,@Param(value = "status") Integer status) throws Exception;
    //在用户表中添加用户信息
    void insertRegisterUser(RegisterUserInfo registerUserInfo);
    //向用户表添加管理员用户信息
    void insertUser(AdminUser adminUser);
    //在密码表添加用户密码
    void insertRegisterPwd(@Param(value = "userId") String userId,@Param(value = "accountNumber")String accountNumber,@Param(value = "password")String password);

    //通过公司id查出邮件通知服务状态
    Integer serviceStateBycompanyUuid(@Param(value = "companyUuid")String companyUuid);

    int selectPlatByPlatId(@Param(value = "platId") String platId,@Param("companyId") String companyId);

    int selectDabbyByIdNum(@Param(value = "idNum") String idNum,@Param(value = "companyId") String companyId);

    int selectDingByDingId(@Param(value = "dingUserId") String dingUserId,@Param(value = "dingUnionId") String dingUnionId,@Param(value = "companyId") String companyId);

    void insertPlat(PlatEwmDomain platEwmDomain);

    void insertDabby(DabbyEwmDomain dabbyEwmDomain);

    void insertDing(DingEwmDomain dingEwmDomain);

    void deleteDabby(@Param(value = "userId") String userId);

    void deletePlat(@Param(value = "userId") String userId);

    void deleteAdBind(@Param(value = "userId")String userId);

    void deleteUserApplication(@Param(value = "userId") String userId);
}
