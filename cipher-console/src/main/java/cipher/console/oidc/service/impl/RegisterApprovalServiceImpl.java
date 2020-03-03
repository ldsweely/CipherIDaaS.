package cipher.console.oidc.service.impl;

import cipher.console.oidc.common.DataGridModel;
import cipher.console.oidc.common.NewReturnUtils;
import cipher.console.oidc.common.ReturnMsg;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.jms.JMSProducer;
import cipher.console.oidc.jms.JMSType;
import cipher.console.oidc.mapper.RegisterApprovalMapper;
import cipher.console.oidc.mapper.UserGroupMapper;
import cipher.console.oidc.service.CheckUserService;
import cipher.console.oidc.service.RegisterApprovalService;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.service.UUIDService;
import cipher.console.oidc.util.NumberUtil;
import cipher.console.oidc.util.aes.AES;
import cipher.console.oidc.util.aes.AesKey;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class RegisterApprovalServiceImpl implements RegisterApprovalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterApprovalServiceImpl.class);

    @Autowired
    private RegisterApprovalMapper registerApprovalMapper;

    @Autowired
    private UUIDService uuidService;

    @Autowired
    private JMSProducer jmsProducer;

    @Override
    public Map<String, Object> getApprovalList(String companyUuid, DataGridModel pageModel) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(companyUuid) && pageModel != null) {
            RegisterApprovalDomain registerApprovalDomain = new RegisterApprovalDomain();
            registerApprovalDomain.setCompanyUuid(companyUuid);
            registerApprovalDomain.setPageData(pageModel);
            int count = registerApprovalMapper.countApprovalListBycompanyUuid(companyUuid);
            Map<String, Object> domain = new HashMap<>();
            if (count > 0) {
                List<RegisterApprovalDomain> registerApprovalDomains = registerApprovalMapper.selectApprovalListBycompanyUuid(registerApprovalDomain);
                if (registerApprovalDomains.size() > 0) {
                    domain.put("rows", registerApprovalDomains);
                    domain.put("total", count);
                    map = NewReturnUtils.successResponse(ReturnMsg.getApprovalListMsg(4));
                    map.put("return_result", domain);
                    return map;
                }
            } else {
                domain.put("rows", "");
                domain.put("total", 0);
                map = NewReturnUtils.successResponse(ReturnMsg.SELECTNO);
                map.put("return_result", domain);
                return map;
            }
        }
        return NewReturnUtils.failureResponse(ReturnMsg.getApprovalListMsg(5));
    }

    @Override
    public Map<String, Object> getRecordsList(String companyUuid, DataGridModel pageModel) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotEmpty(companyUuid) && pageModel != null) {
            RegisterApprovalDomain registerApprovalDomain = new RegisterApprovalDomain();
            registerApprovalDomain.setCompanyUuid(companyUuid);
            registerApprovalDomain.setPageData(pageModel);
            int count = registerApprovalMapper.countRecordsListBycompanyUuid(companyUuid);
            Map<String, Object> domain = new HashMap<>();
            if (count > 0) {
                List<RegisterApprovalDomain> registerApprovalDomains = registerApprovalMapper.selectRecordsListBycompanyUuid(registerApprovalDomain);
                if (registerApprovalDomains.size() > 0) {
                    domain.put("rows", registerApprovalDomains);
                    domain.put("total", count);
                    map = NewReturnUtils.successResponse(ReturnMsg.getRecordsListMsg(4));
                    map.put("return_result", domain);
                    return map;
                }
            } else {
                domain.put("rows", "");
                domain.put("total", 0);
                map = NewReturnUtils.successResponse(ReturnMsg.SELECTNO);
                map.put("return_result", domain);
                return map;
            }
        }
        return NewReturnUtils.failureResponse(ReturnMsg.getRecordsListMsg(5));
    }

    @Autowired
    private UserGroupMapper userGroupMapper;

    @Autowired
    private CheckUserService checkUserService;

    @Override
    public Map<String, Object> getApprovalResult(String companyUuid, String uuid, Integer status) {
        if (StringUtils.isNotEmpty(companyUuid) && StringUtils.isNotEmpty(uuid) && status > 0) {
            if(StringUtils.isNotEmpty(companyUuid) && StringUtils.isNotEmpty(uuid) &&status.equals(2)){
                //修改审批状态
                modifyApproval(companyUuid,uuid,status);
            }else {
                //通过uuid查出注册信息
                RegisterApprovalDomain registerApprovalDomain = registerApprovalMapper.selectRegisterByuuid(uuid);
                Integer flag = checkUserService.checkUserInfo(companyUuid, "", registerApprovalDomain.getUserEmail(), registerApprovalDomain.getPhoneNumber());
                if(flag.equals(1)){
                    return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERACCOUNTNUMBER);
                }else if(flag.equals(2)){
                    return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERMAIL);
                }else if(flag.equals(3)){
                    return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERTELEPHONE);
                }else{
                    Integer service = registerApprovalMapper.serviceStateBycompanyUuid(companyUuid);
                    /*//如果是邮件通知 ，则必须使邮件通知服务开启
                    if(StringUtils.isNotEmpty(registerApprovalDomain.getUserEmail())&&service.equals(1)){
                        return NewReturnUtils.failureResponse(ReturnMsg.APPROVALEMAIL);
                    }*/
                    if (registerApprovalDomain != null && status.equals(1)) {
                        //审批时：赛赋扫码，钉钉扫码，大白扫码 如果发生在该公司下重复，则注册失败
                        boolean platFlag = false;
                        boolean dabbyFlag = false;
                        boolean dingFlag = false;
                        //赛赋扫码是否发生重复
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getPlatId())){
                            int a = registerApprovalMapper.selectPlatByPlatId(registerApprovalDomain.getPlatId(), companyUuid);
                            if(a>0){
                                platFlag = true;
                            }
                        }
                        //大白扫码是否发生重复
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getIdNum())){
                            int b = registerApprovalMapper.selectDabbyByIdNum(registerApprovalDomain.getIdNum(), companyUuid);
                            if(b>0){
                                dabbyFlag = true;
                            }
                        }
                        //钉钉扫码是否发生重复
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getDingUserId())&&StringUtils.isNotEmpty(registerApprovalDomain.getUnionId())){
                            int c = registerApprovalMapper.selectDingByDingId(registerApprovalDomain.getDingUserId(), registerApprovalDomain.getUnionId(), companyUuid);
                            if(c>0){
                                dingFlag = true;
                            }
                        }
                        if(platFlag||dabbyFlag||dingFlag){
                            if(platFlag){
                                return NewReturnUtils.failureResponse(ReturnMsg.PLATEWMAUTH);
                            }
                            if(dabbyFlag){
                                return NewReturnUtils.failureResponse(ReturnMsg.DABBYEWMAUTH);
                            }
                            if(dingFlag){
                                return NewReturnUtils.failureResponse(ReturnMsg.DINGEWMAUTH);
                            }
                        }
                        //没有发生重复继续执行
                        //修改审批状态
                        modifyApproval(companyUuid,uuid,status);

                        String userId = uuidService.getUUid();
                        RegisterUserInfo registerUserInfo = new RegisterUserInfo();
                        registerUserInfo.setUuid(userId);
                        registerUserInfo.setCompanyId(companyUuid);
                        registerUserInfo.setAccountNumber(registerApprovalDomain.getPhoneNumber());
                        registerUserInfo.setUserName(registerApprovalDomain.getUserName());
                        registerUserInfo.setMail(registerApprovalDomain.getUserEmail());
                        registerUserInfo.setPhoneNumber(registerApprovalDomain.getPhoneNumber());
                        //向用户表添加一条用户信息
                        registerApprovalMapper.insertRegisterUser(registerUserInfo);

                        //审批通过后，分别向对应表中添加数据（赛赋扫码，大白扫码，钉钉扫码）
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getPlatId())){
                            PlatEwmDomain platEwmDomain = new PlatEwmDomain();
                            platEwmDomain.setPlatId(registerApprovalDomain.getPlatId());
                            platEwmDomain.setUserId(userId);
                            platEwmDomain.setCompanyId(companyUuid);
                            registerApprovalMapper.insertPlat(platEwmDomain);
                        }
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getIdNum())){
                            DabbyEwmDomain dabbyEwmDomain = new DabbyEwmDomain();
                            dabbyEwmDomain.setUserId(userId);
                            dabbyEwmDomain.setCompanyId(companyUuid);
                            dabbyEwmDomain.setLinkAccount(registerApprovalDomain.getIdNum());
                            registerApprovalMapper.insertDabby(dabbyEwmDomain);
                        }
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getDingUserId())&&StringUtils.isNotEmpty(registerApprovalDomain.getUnionId())){
                            DingEwmDomain dingEwmDomain = new DingEwmDomain();
                            dingEwmDomain.setUserId(userId);
                            dingEwmDomain.setCompanyId(companyUuid);
                            dingEwmDomain.setDingUserId(registerApprovalDomain.getDingUserId());
                            dingEwmDomain.setDingUnionId(registerApprovalDomain.getUnionId());
                            registerApprovalMapper.insertDing(dingEwmDomain);
                        }

                        //向密码表添加一条密码
                        String password = NumberUtil.getStringRandom(8);
                        String encryptPassword = AES.encryptToBase64(password, AesKey.AES_KEY);
                        registerApprovalMapper.insertRegisterPwd(userId, registerApprovalDomain.getPhoneNumber(), encryptPassword);

                        //添加到根节点
                        GroupUserMapDomain groupUserMapDomain=new GroupUserMapDomain();
                        groupUserMapDomain.setUserId(userId);
                        groupUserMapDomain.setGroupId(0);
                        userGroupMapper.insertUserGroupMap(groupUserMapDomain);
                        //发送账号密码，优先邮件（邮件或短信）
                        if(StringUtils.isNotEmpty(registerApprovalDomain.getUserEmail())&&service.equals(0)){
                            //发送邮件
                            EmailAccountInfoDomain emailAccountInfoDomain = new EmailAccountInfoDomain();
                            emailAccountInfoDomain.setCompanyId(companyUuid);
                            emailAccountInfoDomain.setMail(registerApprovalDomain.getUserEmail());
                            emailAccountInfoDomain.setAccount(registerApprovalDomain.getPhoneNumber());
                            emailAccountInfoDomain.setPassword(password);
                            jmsProducer.sendMessage(emailAccountInfoDomain,JMSType.EMAIL_SEND_INFORM);
                        }else{
                            //发送短信
                            SmsAccountInfoDomain smsAccountInfoDomain = new SmsAccountInfoDomain();
                            smsAccountInfoDomain.setAccount(registerApprovalDomain.getPhoneNumber());
                            smsAccountInfoDomain.setPassword(password);
                            smsAccountInfoDomain.setTelephone(registerApprovalDomain.getPhoneNumber());
                            smsAccountInfoDomain.setCompanyId(companyUuid);
                            jmsProducer.sendMessage(smsAccountInfoDomain,JMSType.SMS_SEND_INFORM);
                        }
                    }
                }
            }
        }
        return NewReturnUtils.successResponse(ReturnMsg.APPROVALSUCCESS);
    }

    @Override
    public Map<String, Object> createAdminUser(AdminUser adminUser) {
        //判断账号是否重复
        Integer flag = checkUserService.checkUserInfo(adminUser.getCompanyUuid(), adminUser.getPhone(), adminUser.getEmail(), adminUser.getPhone());
        if(flag.equals(1)){
            return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERACCOUNTNUMBER);
        }else if(flag.equals(2)){
            return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERMAIL);
        }else if(flag.equals(3)){
            return NewReturnUtils.failureResponse(ReturnMsg.CHECKUSERTELEPHONE);
        }else{
            registerApprovalMapper.insertUser(adminUser);
            //向密码表添加一条密码
            String password = "123456";
            String encryptPassword = AES.encryptToBase64(password, AesKey.AES_KEY);
            registerApprovalMapper.insertRegisterPwd(adminUser.getUuid(), adminUser.getPhone(), encryptPassword);
            //添加到根节点
            GroupUserMapDomain groupUserMapDomain=new GroupUserMapDomain();
            groupUserMapDomain.setUserId(adminUser.getUuid());
            groupUserMapDomain.setGroupId(0);
            userGroupMapper.insertUserGroupMap(groupUserMapDomain);
            //发送邮件
            EmailAccountInfoDomain emailAccountInfoDomain = new EmailAccountInfoDomain();
            emailAccountInfoDomain.setCompanyId(adminUser.getCompanyUuid());
            emailAccountInfoDomain.setMail(adminUser.getEmail());
            emailAccountInfoDomain.setAccount(adminUser.getPhone());
            emailAccountInfoDomain.setPassword(password);
            jmsProducer.sendMessage(emailAccountInfoDomain,JMSType.EMAIL_SEND_INFORM);
        }
        return  NewReturnUtils.successResponse(ReturnMsg.APPROVALSUCCESS);
    }

    public void modifyApproval(String companyUuid, String uuid, Integer status){
        if(StringUtils.isNotEmpty(companyUuid)&&StringUtils.isNotEmpty(uuid)&&status>0){
            try {
                //修改注册审批状态
                registerApprovalMapper.ApprovalResult(companyUuid, uuid, status);
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("enter RegisterApprovalServiceImpl.getApprovalResult() but update failed,companyUuid=[{}]" +
                        ",uuid=[{}],status=[{}]..==" + new Object[]{companyUuid, uuid, status});
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    //生产的密码包括位数
    public static void main(String[] args) {
        System.out.println(AES.encryptToBase64("DFSAFF12", AesKey.AES_KEY));
        System.out.println(NumberUtil.getStringRandom(8));
    }

}
