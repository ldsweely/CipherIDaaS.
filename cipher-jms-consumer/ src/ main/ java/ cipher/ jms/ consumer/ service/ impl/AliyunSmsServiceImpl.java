package cipher.jms.consumer.service.impl;

import cipher.jms.consumer.common.CacheKey;
import cipher.jms.consumer.common.Constants;
import cipher.jms.consumer.domain.AliyunSmsConfigInfo;
import cipher.jms.consumer.domain.SmsAccountInfoDomain;
import cipher.jms.consumer.domain.SmsChannelInfo;
import cipher.jms.consumer.mapper.SmsChannelInfoMapper;
import cipher.jms.consumer.redis.RedisClient;
import cipher.jms.consumer.service.AliyunSmsService;
import cipher.jms.consumer.util.AccountValidatorUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySendDetailsResponse;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


@Service
public class AliyunSmsServiceImpl implements AliyunSmsService{



    private static final Logger logger     = LoggerFactory.getLogger(AliyunSmsServiceImpl.class);

    @Autowired
    private RedisClient<String, Object> redisTemplate   = new RedisClient<String, Object>();

    @Autowired
    private SmsChannelInfoMapper smsChannelInfoMapper;
    @Override
    public String getAliyunSendMobilePhoneSmsStr(String phone,String companyId) {
        if (StringUtils.isBlank(phone)){
            return null;
        }
        if (!AccountValidatorUtil.isMobile(phone)){
            return null;
        }
        int smsCode = (int) (Math.random() * 1000000);
        String smsCodeStr = String.format("%06d", smsCode);
        // 放入缓存，10分钟失效
        try {
            AliyunSmsConfigInfo aliyunSmsConfigInfo = (AliyunSmsConfigInfo)redisTemplate.get(CacheKey.getCacheKeyAliYunInfo());
            if(null==aliyunSmsConfigInfo){
                aliyunSmsConfigInfo= getAliyunSmsConfigInfo(companyId);
                redisTemplate.put(CacheKey.getCacheKeyAliYunInfo(),aliyunSmsConfigInfo);
            }
            logger.info("getAliyunSendMobilePhoneSmsStr info===..", "phone=" + phone + "smsCodeStr= " + smsCodeStr);
            SendSmsResponse response=sendAliSmsCode(phone,smsCodeStr,aliyunSmsConfigInfo);
            if (!"OK".equals(response.getCode())) {
                // 异常返回输出错误码和错误信息
                logger.info("getAliyunSendMobilePhoneSmsStr error===..", "错误码=" + response.getCode() + " 错误信息= " + response.getMessage());
                return null;
            }
            //放入缓存，5分钟失效
            redisTemplate.put(CacheKey.getMobilePhoneSmsStrCacheKey(phone), smsCodeStr, ttl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return smsCodeStr;
    }

    @Override
    public String getAliyunSendMobilePhoneSmsInformStr(SmsAccountInfoDomain smsAccountInfoDomain) {
        if(smsAccountInfoDomain!=null){
            try {
                AliyunSmsConfigInfo aliyunSmsConfigInfo = (AliyunSmsConfigInfo)redisTemplate.get(CacheKey.getCacheKeyAliYunInfo());
                if(null==aliyunSmsConfigInfo){
                    aliyunSmsConfigInfo= getAliyunSmsConfigInfo(smsAccountInfoDomain.getCompanyId());
                    redisTemplate.put(CacheKey.getCacheKeyAliYunInfo(),aliyunSmsConfigInfo);
                }
                logger.error("getAliyunSendMobilePhoneSmsInformStr info===..", "telephone=" +smsAccountInfoDomain.getTelephone() + "accoutn=" +smsAccountInfoDomain.getAccount()+"password="+smsAccountInfoDomain.getPassword());
                SendSmsResponse response=sendAliyunSendMobilePhoneSmsInformStr(smsAccountInfoDomain,aliyunSmsConfigInfo);
                if (!"OK".equals(response.getCode())) {
                    // 异常返回输出错误码和错误信息
                    logger.error("getAliyunSendMobilePhoneSmsStr error===..", "错误码=" + response.getCode() + " 错误信息= " + response.getMessage());
                    return null;
                }
            } catch (ClientException e) {
                e.printStackTrace();
            }
        }
        return "account= " + smsAccountInfoDomain.getAccount()+",password="+smsAccountInfoDomain.getPassword();
    }


    /*public static SendSmsResponse sendAliSmsCode(String telephone,String code,AliyunSmsConfigInfo aliyunSmsConfigInfo) throws ClientException {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "50000");
        System.setProperty("sun.net.client.defaultReadTimeout", "50000");
        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunSmsConfigInfo.getAccessKeyId(), aliyunSmsConfigInfo.getAccessKeySecret());
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", aliyunSmsConfigInfo.getProduct(), aliyunSmsConfigInfo.getDomain());
        IAcsClient acsClient = new DefaultAcsClient(profile);
        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers(telephone);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(aliyunSmsConfigInfo.getSignName());
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode(aliyunSmsConfigInfo.getTemplateCode());
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{" + "\"code\":\""+ code +"\"" + "}");
        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
        return sendSmsResponse;
    }
    public static SendSmsResponse sendAliSmsCode(SmsAccountInfoDomain smsAccountInfoDomain,AliyunSmsConfigInfo aliyunSmsConfigInfo) throws ClientException {
        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "50000");
        System.setProperty("sun.net.client.defaultReadTimeout", "50000");
        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunSmsConfigInfo.getAccessKeyId(), aliyunSmsConfigInfo.getAccessKeySecret());
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", aliyunSmsConfigInfo.getProduct(), aliyunSmsConfigInfo.getDomain());
        IAcsClient acsClient = new DefaultAcsClient(profile);
        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers(smsAccountInfoDomain.getTelephone());
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(aliyunSmsConfigInfo.getSignName());
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode(aliyunSmsConfigInfo.getTemplateCode());
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{" + "\"account\":\""+smsAccountInfoDomain.getAccount()+"\",\"password\":\""+smsAccountInfoDomain.getPassword()+"\",\"time\":\"10分钟\"}");
        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);
        return sendSmsResponse;
    }
*/


    public static SendSmsResponse sendAliyunSendMobilePhoneSmsInformStr(SmsAccountInfoDomain smsAccountInfoDomain,AliyunSmsConfigInfo aliyunSmsConfigInfo) throws ClientException {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "50000");
        System.setProperty("sun.net.client.defaultReadTimeout", "50000");

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunSmsConfigInfo.getAccessKeyId(), aliyunSmsConfigInfo.getAccessKeySecret());
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", aliyunSmsConfigInfo.getProduct(), aliyunSmsConfigInfo.getDomain());
        IAcsClient acsClient = new DefaultAcsClient(profile);

        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers(smsAccountInfoDomain.getTelephone());
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(aliyunSmsConfigInfo.getSignName());
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode(aliyunSmsConfigInfo.getTemplateCode());
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{" + "\"account\":\""+smsAccountInfoDomain.getAccount()+"\",\"password\":\""+smsAccountInfoDomain.getPassword()+"\",\"time\":\"10分钟\"}");

        //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
        //request.setSmsUpExtendCode("90997");

        //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
        request.setOutId("yourOutId");

        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);

        return sendSmsResponse;
    }




    public static SendSmsResponse sendAliSmsCode(String phone,String smsCodeStr,AliyunSmsConfigInfo aliyunSmsConfigInfo) {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "50000");
        System.setProperty("sun.net.client.defaultReadTimeout", "50000");

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunSmsConfigInfo.getAccessKeyId(), aliyunSmsConfigInfo.getAccessKeySecret());
        try {
            DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", aliyunSmsConfigInfo.getProduct(), aliyunSmsConfigInfo.getDomain());
        } catch (ClientException e) {
            e.printStackTrace();
        }
        IAcsClient acsClient = new DefaultAcsClient(profile);

        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers(phone);
        //必填:短信签名-可在短信控制台中找到
        request.setSignName(aliyunSmsConfigInfo.getSignName());
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode(aliyunSmsConfigInfo.getTemplateCode());
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{" + "\"code\":\""+smsCodeStr+"\"}");

        //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
        //request.setSmsUpExtendCode("90997");

        //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
        request.setOutId("yourOutId");

        //hint 此处可能会抛出异常，注意catch
        SendSmsResponse sendSmsResponse = null;
        try {
            sendSmsResponse = acsClient.getAcsResponse(request);
        } catch (ClientException e) {
            e.printStackTrace();
        }

        return sendSmsResponse;
    }





    public static QuerySendDetailsResponse querySendDetails(String bizId,SmsAccountInfoDomain smsAccountInfoDomain,AliyunSmsConfigInfo aliyunSmsConfigInfo) throws ClientException {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "50000");
        System.setProperty("sun.net.client.defaultReadTimeout", "50000");

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", aliyunSmsConfigInfo.getAccessKeyId(), aliyunSmsConfigInfo.getAccessKeySecret());
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", aliyunSmsConfigInfo.getProduct(), aliyunSmsConfigInfo.getDomain());
        IAcsClient acsClient = new DefaultAcsClient(profile);

        //组装请求对象
        QuerySendDetailsRequest request = new QuerySendDetailsRequest();
        //必填-号码
        request.setPhoneNumber(smsAccountInfoDomain.getTelephone());
        //可选-流水号
        request.setBizId(bizId);
        //必填-发送日期 支持30天内记录查询，格式yyyyMMdd
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
        request.setSendDate(ft.format(new Date()));
        //必填-页大小
        request.setPageSize(10L);
        //必填-当前页码从1开始计数
        request.setCurrentPage(1L);

        //hint 此处可能会抛出异常，注意catch
        QuerySendDetailsResponse querySendDetailsResponse = acsClient.getAcsResponse(request);

        return querySendDetailsResponse;
    }







    public static SendSmsResponse sendSms() throws ClientException {

        //可自助调整超时时间
        System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
        System.setProperty("sun.net.client.defaultReadTimeout", "10000");

        //初始化acsClient,暂不支持region化
        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);
        IAcsClient acsClient = new DefaultAcsClient(profile);

        //组装请求对象-具体描述见控制台-文档部分内容
        SendSmsRequest request = new SendSmsRequest();
        //必填:待发送手机号
        request.setPhoneNumbers("13221071988");
        //必填:短信签名-可在短信控制台中找到
        request.setSignName("赛赋科技");
        //必填:短信模板-可在短信控制台中找到
        request.setTemplateCode("SMS_153990932");
        //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
        request.setTemplateParam("{\"code\":\"123\"}");

        //选填-上行短信扩展码(无特殊需求用户请忽略此字段)
        //request.setSmsUpExtendCode("90997");

        //可选:outId为提供给业务方扩展字段,最终在短信回执消息中将此值带回给调用者
        request.setOutId("yourOutId");

        //hint 此处可能会抛出异常，注意catch

        SendSmsResponse sendSmsResponse = acsClient.getAcsResponse(request);

        return sendSmsResponse;
    }











    /**
     * @param template 尊敬的用户,您在{platform}的验证码已发送！
     * @param param    键值对
     * @return
     */
    public static String convertMessage(String template, Map<String, String> param) {
        if (template != null && param != null && !param.isEmpty()) {
            for (Map.Entry<String, String> entry : param.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return template;
    }

    public AliyunSmsConfigInfo getAliyunSmsConfigInfo(String companyId) {
        SmsChannelInfo record = new SmsChannelInfo();
        record.setCompanyId(companyId);
        SmsChannelInfo smsChannelInfo = smsChannelInfoMapper.getSmsChannelInfo(record);
        AliyunSmsConfigInfo aliyunSmsConfigInfo = new AliyunSmsConfigInfo();
        if (null != smsChannelInfo && StringUtils.isNotEmpty(String.valueOf(smsChannelInfo.getParameter()))) {
            JSONObject jsonObject = JSONObject.fromObject(smsChannelInfo.getParameter());
            aliyunSmsConfigInfo = (AliyunSmsConfigInfo) JSONObject.toBean(jsonObject, AliyunSmsConfigInfo.class);
        }
        return  aliyunSmsConfigInfo;
    }



    public  static  void convertObject() {
        AliyunSmsConfigInfo aliyunSmsConfigInfo=new AliyunSmsConfigInfo();
        aliyunSmsConfigInfo.setAccessKeyId(accessKeyId);
        aliyunSmsConfigInfo.setAccessKeySecret(accessKeySecret);
        aliyunSmsConfigInfo.setDomain(domain);
        aliyunSmsConfigInfo.setProduct(product);
        aliyunSmsConfigInfo.setSignName(signName);
        aliyunSmsConfigInfo.setTemplateCode(templeteCode);
        //1、使用JSONObject
        JSONObject json = JSONObject.fromObject(aliyunSmsConfigInfo);
        //2、使用JSONArray
        JSONArray array= JSONArray.fromObject(aliyunSmsConfigInfo);
        String strJson=json.toString();
        String strArray=array.toString();
        System.out.println("strJson:"+strJson);
        System.out.println("strArray:"+strArray);
    }

   

}
