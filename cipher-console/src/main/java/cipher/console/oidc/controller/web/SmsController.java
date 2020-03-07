package cipher.console.oidc.controller.web;

import cipher.console.oidc.common.ConstantsCMP;
import cipher.console.oidc.domain.web.*;
import cipher.console.oidc.enums.AdminBehaviorEnum;
import cipher.console.oidc.jms.JMSProducer;
import cipher.console.oidc.jms.JMSType;
import cipher.console.oidc.redis.RedisClient;
import cipher.console.oidc.service.AdminBehaviorInfoService;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.service.SmsChannelInfoService;
import cipher.console.oidc.service.SmsInfoService;
import cipher.console.oidc.util.IpUtil;
import cipher.console.oidc.util.ResponseUtils;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static cipher.console.oidc.common.ReturnUtils.successResponse;

/**
 * Created by 95744 on 2018/8/23.
 */

@Controller
@RequestMapping(value = "/cipher/sms")
@EnableAutoConfiguration
public class SmsController {

    private static final String CACHE_KEY_RONG_LIAN_INFO = "KEY_RONG_LIAN_INFO";

    @Autowired
    private SmsInfoService smsInfoService;

    @Autowired
    private SmsChannelInfoService smsChannelInfoService;


    @Autowired
    private AdminBehaviorInfoService adminBehaviorInfoService;

    @Autowired
    private JMSProducer jmsProducer;

    @Autowired
    private RedisClient<String, Object> redisClient;




    @Autowired
    private SessionService sessionService;

    private static final Logger logger = LoggerFactory.getLogger(SmsController.class);

    @RequestMapping(value = "/set")
    public String pageSet() {
        return "sms/set";
    }


    /**
     * 查询已有的信息
     */
    @RequestMapping(value = "/list", params = "json", method = RequestMethod.POST)
    @ResponseBody
    public SmsInfoDomain queryDataInfo(HttpServletRequest request) {
        logger.debug("enter SmsController.queryDataInfo");
        SmsInfoDomain smsInfoDomain = new SmsInfoDomain();
        String companyUuid = sessionService.getCompanyUUid(request.getSession());
        smsInfoDomain.setCompanyId(companyUuid);
        return smsInfoService.getSmsInfo(smsInfoDomain);
    }


    @RequestMapping(value = "/update")
    public void insertSmsInfo(HttpServletResponse response, HttpServletRequest request, HttpSession session, SmsInfoDomain form) {
        logger.debug("enter SmsController.insertSmsInfo");
        SmsInfoDomain smsInfoDomain = smsInfoService.getSmsInfo(form);
        String companyUuid = sessionService.getCompanyUUid(request.getSession());
        form.setCompanyId(companyUuid);
        try {
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),  "更新短信设置");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (ObjectUtils.isEmpty(smsInfoDomain)) {
            smsInfoService.insertSmsInfo(form);
            ResponseUtils.customSuccessResponse(response, "保存配置信息成功");
        }
        try {
            smsInfoService.updateSmsInfo(form);
            ResponseUtils.customSuccessResponse(response, "更新配置信息成功");
        } catch (Exception e) {
            logger.error("enter SmsController.insertSmsInfo; Error:" + e.getCause());
            ResponseUtils.customFailueResponse(response, "更新配置失败，请稍后重试");
            e.printStackTrace();
        }
    }


    @RequestMapping(value = "/setNew")
    @ResponseBody
    public Map<String, Object> pageSetNew() {
        //TODO
//        ModelAndView res=new ModelAndView("sms/setNew");
        SmsChannelInfo record = new SmsChannelInfo();
        SmsChannelInfo smsChannelInfo = smsChannelInfoService.getSmsChannelInfo(record);
        return successResponse("smsChannelInfo", smsChannelInfo);

    }


    @RequestMapping(value = "/listNew", params = "json", method = RequestMethod.POST)
    @ResponseBody
    public SmsChannelInfo queryDataInfoNew(HttpServletRequest request, SmsChannelInfo smsChannelInfo) {
        logger.debug("enter SmsController.queryDataInfoNew");
        String companyUuid = sessionService.getCompanyUUid(request.getSession());
        smsChannelInfo.setCompanyId(companyUuid);
        if (null == smsChannelInfo.getType()) {
            SmsChannelInfo record = new SmsChannelInfo();
            record.setCompanyId(companyUuid);
            smsChannelInfo = smsChannelInfoService.getSmsChannelInfo(record);
        } else {
            smsChannelInfo = smsChannelInfoService.selectSmsChannel(smsChannelInfo);
        }
        JSONObject jsonobject = JSONObject.fromObject(smsChannelInfo.getParameter());
        if (smsChannelInfo.getType() == 1) {
            RonglianSmsConfigInfo ronglianSmsConfigInfo = (RonglianSmsConfigInfo) JSONObject.toBean(jsonobject, RonglianSmsConfigInfo.class);
            smsChannelInfo.setRonglianSmsConfigInfo(ronglianSmsConfigInfo);
        } else {
            AliyunSmsConfigInfo aliyunSmsConfigInfo = (AliyunSmsConfigInfo) JSONObject.toBean(jsonobject, AliyunSmsConfigInfo.class);
            smsChannelInfo.setAliyunSmsConfigInfo(aliyunSmsConfigInfo);
        }
        return smsChannelInfo;
    }


    @RequestMapping(value = "/updateNew")
    public void updateNew(HttpServletResponse response, HttpServletRequest request,HttpSession session,
                          SmsChannelInfo form, RonglianSmsConfigInfo ronglianSmsConfigInfo,
                          AliyunSmsConfigInfo aliyunSmsConfigInfo) {
        logger.debug("enter SmsController.updateNew");
        redisClient.remove(CACHE_KEY_RONG_LIAN_INFO);
        SmsChannelInfo smsChannelInfo = smsChannelInfoService.getSmsChannelInfo(form);
        try {
            String companyId=ConstantsCMP.getSessionCompanyId(request);
            AdminBehaviorInfo adminBehaviorInfo = new AdminBehaviorInfo(AdminBehaviorEnum.SYSTEM_MANAGER.getType(),  "更新短信设置");
            adminBehaviorInfoService.insertSelective(adminBehaviorInfo,session);

            form.setCompanyId(companyId);
            if (ObjectUtils.isEmpty(smsChannelInfo)) {
                smsChannelInfoService.insertSmsChannelInfo(form);
                ResponseUtils.customSuccessResponse(response, "保存配置信息成功");
            } else {
                form.setState(ConstantsCMP.SmsConstant.IS_CLOSE);
                smsChannelInfoService.updateByKey(form);
                smsChannelInfo = smsChannelInfoService.selectSmsChannel(form);
                SmsChannelInfo record = new SmsChannelInfo();
                record.setCompanyId(companyId);
                JSONObject jsonvistor;
                if (smsChannelInfo.getType() == 1) {
                    jsonvistor = JSONObject.fromObject(ronglianSmsConfigInfo);//将java对象转换为json对象
                } else {
                    jsonvistor = JSONObject.fromObject(aliyunSmsConfigInfo);//将java对象转换为json对象
                }
                record.setId(smsChannelInfo.getId());
                String staffstr = jsonvistor.toString();//将json对象转换为字符串
                record.setParameter(staffstr);
                record.setState(ConstantsCMP.SmsConstant.IS_OPEN);
                smsChannelInfoService.updateSmsChannelInfo(record);
                ResponseUtils.customSuccessResponse(response, "更新配置信息成功");
            }
        } catch (Exception e) {
            logger.error("enter SmsController.updateNew; Error:" + e.getCause());
            ResponseUtils.customFailueResponse(response, "更新配置失败，请稍后重试");
            e.printStackTrace();
        }
    }


    @RequestMapping(value = "/common", params = "json", method = RequestMethod.POST)
    @ResponseBody
    public List<SmsChannelInfo> getList(HttpServletRequest request) {
        logger.debug("enter SmsController.getList");
        String companyId = ConstantsCMP.getSessionCompanyId(request);
        SmsChannelInfo smsChannelInfo = new SmsChannelInfo();
        smsChannelInfo.setCompanyId(companyId);
        return smsChannelInfoService.getSmsChannelList(smsChannelInfo);
    }


    /**
     * 测试验证码短信发送
     * @param request
     * @param response
     * @param telephone
     */
    @RequestMapping(value = "/sendMsg", method = RequestMethod.POST)
    @ResponseBody
    public void sendMsg(HttpServletRequest request,HttpServletResponse response, @RequestParam(value = "telephone") String telephone) {
        logger.debug("enter SmsController.sendMsg");
       String companyId = sessionService.getCompanyUUid(request.getSession());

        //发送短信
        try {
            SmsCodeInfoDomain smsCodeInfoDomain = new SmsCodeInfoDomain(telephone,companyId);
            jmsProducer.sendMessage(smsCodeInfoDomain, JMSType.SMS_SEND_SERVICE);
            ResponseUtils.customSuccessResponse(response, "发送短信成功");
        } catch (Exception e) {
            ResponseUtils.customSuccessResponse(response, "服务器错误");
        }
    }

    /**
     * 测试开通账号短信发送
     * @param request
     * @param response
     * @param telephone
     */
    @RequestMapping(value = "/sendUserMsg", method = RequestMethod.POST)
    @ResponseBody
    public void sendUserMsg(HttpServletRequest request,HttpServletResponse response, @RequestParam(value = "telephone") String telephone) {
        logger.debug("enter SmsController.sendUserMsg");
        String companyId = sessionService.getCompanyUUid(request.getSession());
        //发送短信
        try {
            SmsAccountInfoDomain smsAccountInfoDomain = new SmsAccountInfoDomain();
            smsAccountInfoDomain.setAccount("cipher");
            smsAccountInfoDomain.setPassword("123456");
            smsAccountInfoDomain.setTelephone(telephone);
            smsAccountInfoDomain.setCompanyId(companyId);
            jmsProducer.sendMessage(smsAccountInfoDomain,JMSType.SMS_SEND_INFORM);
            ResponseUtils.customSuccessResponse(response, "发送短信成功");
        } catch (Exception e) {
            ResponseUtils.customSuccessResponse(response, "服务器错误");
        }
    }
}
