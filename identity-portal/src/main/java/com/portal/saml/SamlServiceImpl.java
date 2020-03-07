package com.portal.saml;

import com.portal.commons.Constants;
import com.portal.domain.ApplicationInfoDomain;
import com.portal.domain.PortalApplyInfo;
import com.portal.domain.UserInfoDomain;
import com.portal.saml.entity.SamlEntity;
import com.portal.saml.utils.AlgorithmMethod;
import com.portal.saml.utils.DigestMethod;
import com.portal.saml.utils.IDPCredentials;
import com.portal.service.ApplicationService;
import com.portal.service.PortalService;
import com.portal.service.SamlService;
import com.portal.service.UserInfoService;
import com.portal.utils.ResponseUtils;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.joda.time.DateTime;
import org.opensaml.core.xml.schema.XSAny;
import org.opensaml.core.xml.schema.impl.XSAnyBuilder;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.messaging.context.*;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.binding.security.impl.SAML2HTTPRedirectDeflateSignatureSecurityHandler;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SPSSODescriptor;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.UsageType;
import org.opensaml.security.credential.impl.CollectionCredentialResolver;
import org.opensaml.xmlsec.SignatureValidationParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.keyinfo.impl.BasicProviderKeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.KeyInfoProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.DSAKeyValueProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.InlineX509DataProvider;
import org.opensaml.xmlsec.keyinfo.impl.provider.RSAKeyValueProvider;
import org.opensaml.xmlsec.signature.support.SignatureTrustEngine;
import org.opensaml.xmlsec.signature.support.impl.ExplicitKeySignatureTrustEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Decoder;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static com.portal.enums.ResultCode.APPLICATION_RULE_NOT_EXIT;
import static org.springframework.util.StringUtils.hasText;

/**
 * TODO:
 * create by shizhao at 2019-12-12
 *
 * @author: shizhao
 * @since: 2019-12-12 10:59
 */
@Service
public class SamlServiceImpl implements SamlService {

    /**
     * 判断用户是不理已经登陆的key
     */
    private String sessionKey = "loginKey";

    /**
     * 不需要修改的字段
     */
    private static final String TEMPLATE_PATH = "/templates/saml2-post-binding.vm";

    private Logger logger= LoggerFactory.getLogger(SamlServiceImpl.class);

    @Autowired
    private ApplicationService applicationService;

    @Resource
    private OpenSamlImplementation openSamlImplementation;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private PortalService portalService;

    @Override
    public Boolean isSamlRegistInLocal(HttpServletRequest request,String samlRequest) {

        //判断入参
        if (StringUtils.isEmpty(samlRequest)){
            logger.info("samlRequest参数为空");
            return false;
        }

        //decode saml的参数
        BASE64Decoder decoder = new BASE64Decoder();
        try {
            String samlEncode=new String(decoder.decodeBuffer(samlRequest), "UTF-8");
        } catch (IOException e) {
            logger.info("samlRequst转String 失败 ");
            logger.error(e.getMessage(),e);
            return false;
        }
        //生成saml的Authrequest对象
        AuthnRequest authnRequest = (AuthnRequest)openSamlImplementation.transferXML2SAMLObject(samlRequest);
        if (validateSignature(request, authnRequest , authnRequest.getIssuer().getValue())){
            return true;
        }
        return false;
    }

    @Override
    public Boolean sendIdentityInfo(SamlEntity samlEntity) {


        HttpServletRequest httpServletRequest=samlEntity.getRequest();
        HttpServletResponse httpServletResponse = samlEntity.getResponse();

        Response response = buildResponse(samlEntity);
        //生成saml的Authrequest对象
        AuthnRequest authnRequest = (AuthnRequest)openSamlImplementation.transferXML2SAMLObject(samlEntity.getSaml());
        String acsUrl = authnRequest.getAssertionConsumerServiceURL();
        openSamlImplementation.signObject(response, AlgorithmMethod.RSA_SHA256, DigestMethod.RIPEMD160,samlEntity.getApplicationInfoDomain());
        String relayState="";
        if (StringUtils.isEmpty(samlEntity.getRelayState())){
            relayState=samlEntity.getApplicationInfoDomain().getRelayState();
        }
        relayState=samlEntity.getRelayState();

        System.out.println(response.toString());
        if (httpPostBinding(relayState, httpServletResponse,acsUrl, response)){
         return true;
        }
        return false;
    }

    /**
     * 只适用于 SAML2HTTPRedirectDeflate的方式,对于POST不支持
     * @param httpServletRequest
     * @param authnRequest
     * @param spEntityId
     * @throws ComponentInitializationException
     * @throws MessageHandlerException
     * @throws IOException
     */
    public Boolean validateSignature(HttpServletRequest httpServletRequest, AuthnRequest authnRequest, String spEntityId) {

        String acs=authnRequest.getAssertionConsumerServiceURL();

        ApplicationInfoDomain applicationInfoDomain=applicationService.getApplicationInfoByAssertionConsumerServiceURL(acs);
        if (applicationInfoDomain ==null){
            logger.info("未找到添加应用");
            return false;
        }
        MessageContext context = new MessageContext();
        context.setMessage(authnRequest);

        SAMLMessageInfoContext samlMessageInfoContext = context.getSubcontext(SAMLMessageInfoContext.class,true);
        samlMessageInfoContext.setMessageIssueInstant(authnRequest.getIssueInstant());


        SAMLPeerEntityContext samlPeerEntityContext = context.getSubcontext(SAMLPeerEntityContext.class, true);
        samlPeerEntityContext.setRole(SPSSODescriptor.DEFAULT_ELEMENT_NAME);


        SAMLProtocolContext samlProtocolContext = context.getSubcontext(SAMLProtocolContext.class, true);
        samlProtocolContext.setProtocol(SAMLConstants.SAML2_REDIRECT_BINDING_URI);

        SecurityParametersContext securityParametersContext = context.getSubcontext(SecurityParametersContext.class,true);

        SignatureValidationParameters signatureValidationParameters = new SignatureValidationParameters();

        //TODO 解析发送请求的 SP 的元数据中的证书形成 Credential

        ArrayList<KeyInfoProvider> providers = new ArrayList<KeyInfoProvider>();
        providers.add( new RSAKeyValueProvider() );
        providers.add( new DSAKeyValueProvider() );
        providers.add( new InlineX509DataProvider() );
        BasicProviderKeyInfoCredentialResolver resolver = new BasicProviderKeyInfoCredentialResolver(providers);
        Credential signCer = null;
        try {
            signCer = readSPCredential(applicationInfoDomain);
        } catch (IOException e) {
            logger.info("创建签名对象失败",signCer);
            logger.error(e.getMessage(),e);
            return false;
        }
        ((BasicCredential)signCer).setUsageType(UsageType.SIGNING);
        ((BasicCredential)signCer).setEntityId(spEntityId);
        CollectionCredentialResolver credentialResolver = new CollectionCredentialResolver(Arrays.asList(signCer));

        SignatureTrustEngine signatureTrustEngine = new ExplicitKeySignatureTrustEngine(credentialResolver, resolver);
        signatureValidationParameters.setSignatureTrustEngine(signatureTrustEngine);
        securityParametersContext.setSignatureValidationParameters(signatureValidationParameters);

        SAML2HTTPRedirectDeflateSignatureSecurityHandler securityHandler = new SAML2HTTPRedirectDeflateSignatureSecurityHandler();
        securityHandler.setHttpServletRequest(httpServletRequest);
        try {
            securityHandler.initialize();
        } catch (ComponentInitializationException e) {
            logger.info("签名校验失败");
            logger.error(e.getMessage(),e);
            return false;
        }
        try {
            securityHandler.invoke(context);
        } catch (MessageHandlerException e) {
            logger.info("签名校验失败");
            logger.error(e.getMessage(),e);
            return false;
        }
        return true;
    }

    /**
     * 这个就是IOT的证书,通过元数据可以获取 地址 https://account.aliplus.com/saml/sp/metadata
     * @return
     * @throws IOException
     */
    public Credential readSPCredential(ApplicationInfoDomain applicationInfoDomain) throws IOException {
        return IDPCredentials.readCredential(applicationInfoDomain);
    }

    /**
     * 免登录的关键代码
     ** @return
     */
    private Response buildResponse(SamlEntity samlEntity) {
        //生成saml的Authrequest对象
        AuthnRequest authnRequest = (AuthnRequest)openSamlImplementation.transferXML2SAMLObject(samlEntity.getSaml());
        String acsUrl = authnRequest.getAssertionConsumerServiceURL();
        String reqId = authnRequest.getID();
        String messageId = OpenSamlImplementation.generateSecureRandomId();
        //进行业务逻辑判断要登录的名称
        String defaultName = samlEntity.getSubAccout();
        Assertion assertion = openSamlImplementation.buildSAMLObject(Assertion.class);
        DateTime now = new DateTime();
        // 断言相关,随便生成的字符串
        assertion.setID(messageId);
        //必须元素,代表要登录iot平台的账号主体
        Subject subject = openSamlImplementation.buildSAMLObject(Subject.class);
        //必须元素,代表要登录的账号主要的用户名
        NameID nameID = openSamlImplementation.buildSAMLObject(NameID.class);
        nameID.setValue(samlEntity.getSubAccout());
        nameID.setFormat(NameIDType.PERSISTENT);
        subject.setNameID(nameID);
        //必须元素 SubjectConfirmationData 的 Method统一为 METHOD_BEARER
        SubjectConfirmation subjectConfirmation = openSamlImplementation.buildSAMLObject(SubjectConfirmation.class);
        SubjectConfirmationData subjectConfirmationData = openSamlImplementation.buildSAMLObject(SubjectConfirmationData.class);
        if(StringUtils.isNotBlank(reqId)) {
            subjectConfirmationData.setInResponseTo(reqId);
        }
        subjectConfirmationData.setNotOnOrAfter(now.plusMinutes(Integer.parseInt(samlEntity.getApplicationInfoDomain().getExitTime())));
        //Recipient设置为IOT的域名
        subjectConfirmationData.setRecipient(acsUrl);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subjectConfirmation.setMethod(SubjectConfirmation.METHOD_BEARER);
        subject.getSubjectConfirmations().add(subjectConfirmation);
        assertion.setSubject(subject);
        assertion.getAuthnStatements().add(getAuthnStatement(messageId));
        assertion.setIssueInstant(now);
        //issuer的值与entityId一致 必须元素
        assertion.setIssuer(idpBuildIssuer(samlEntity));
        assertion.setIssueInstant(now);
        //必须元素
        Conditions conditions = openSamlImplementation.buildSAMLObject(Conditions.class);
        conditions.setNotBefore(now);
        conditions.setNotOnOrAfter(now.plusMinutes(5));
        AudienceRestriction audienceRestriction = openSamlImplementation.buildSAMLObject(AudienceRestriction.class);
//        //必须元素
        Audience audience = openSamlImplementation.buildSAMLObject(Audience.class);
        //固定
        audience.setAudienceURI(authnRequest.getIssuer().getValue());
        audienceRestriction.getAudiences().add(audience);
        conditions.getAudienceRestrictions().add(audienceRestriction);
        assertion.setConditions(conditions);

        //名称为 companyId 是 Attribute 是必须元素,代表公司ID
        AttributeStatement attributeStatement =  openSamlImplementation.buildSAMLObject(AttributeStatement.class);
        Attribute attribute = openSamlImplementation.buildSAMLObject(Attribute.class);
        attribute.setName("applicatiionId");
        XSAny attributeValue =  new XSAnyBuilder().buildObject(AttributeValue.DEFAULT_ELEMENT_NAME);
        attributeValue.setTextContent(samlEntity.getApplicationInfoDomain().getApplicationId());
        attribute.getAttributeValues().add(attributeValue);
        attributeStatement.getAttributes().add(attribute);

        assertion.getAttributeStatements().add(attributeStatement);

        Response response = openSamlImplementation.buildSAMLObject(Response.class);
        response.setID(OpenSamlImplementation.generateSecureRandomId());
        Status status = openSamlImplementation.buildSAMLObject(Status.class);
        StatusCode statusCode = openSamlImplementation.buildSAMLObject(StatusCode.class);
        //Status Code 要设置成SUCEESS
        statusCode.setValue(StatusCode.SUCCESS);
        status.setStatusCode(statusCode);

        response.setStatus(status);
        //DESTION设置成IOT的ACS
        response.setDestination(acsUrl);
        response.getAssertions().add(assertion);
        response.setIssueInstant(now);
        response.setIssuer(this.idpBuildIssuer(samlEntity));
        response.setVersion(SAMLVersion.VERSION_20);
        //对断言加签
        openSamlImplementation.signObject(assertion, AlgorithmMethod.RSA_SHA256, DigestMethod.RIPEMD160,samlEntity.getApplicationInfoDomain());
        return response;
    }

    private AuthnStatement getAuthnStatement(String msgId){
        AuthnStatement authnStatement = openSamlImplementation.buildSAMLObject(AuthnStatement.class);
        AuthnContext authnContext = openSamlImplementation.buildSAMLObject(AuthnContext.class);
        AuthnContextClassRef authnContextClassRef = openSamlImplementation.buildSAMLObject(AuthnContextClassRef.class);
        authnContextClassRef.setAuthnContextClassRef(AuthnContext.PASSWORD_AUTHN_CTX);
        authnContext.setAuthnContextClassRef(authnContextClassRef);
        authnStatement.setAuthnContext(authnContext);
        authnStatement.setAuthnInstant(new DateTime());
        //当从 SP 登出时 需要通过 SessionIndex 来确定出会话
        authnStatement.setSessionIndex(msgId);

        return authnStatement;
    }

    public Issuer idpBuildIssuer(SamlEntity samlEntity) {
        Issuer issuer = openSamlImplementation.buildSAMLObject(Issuer.class);
        String idpEntityId = samlEntity.getApplicationInfoDomain().getEntityId();
        issuer.setValue(idpEntityId);
        return issuer;
    }

    /**
     * HTTP POST BINDING 时用于编码返回结果并返回给浏览器
     * 使用其他方式返回时可以使用
     * 目前iot只支持 HTTPPostEncoder
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPArtifactEncoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPRedirectDeflateEncoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HTTPSOAP11Encoder}
     * {@link org.opensaml.saml.saml2.binding.encoding.impl.HttpClientRequestSOAP11Encoder}
     * 等上类实现
     * @param relayState
     * @param res
     * @param acsUrl
     * @param response
     * @throws ComponentInitializationException
     * @throws MessageEncodingException
     */
    private Boolean httpPostBinding(String relayState,
                                 HttpServletResponse res, String acsUrl, Response response) {
        // HTTP相关的类不放到 openSamlImplementation 中
        MessageContext messageContext = new MessageContext();
        messageContext.setMessage(response);
        if(hasText(relayState)) {
            messageContext.getSubcontext(SAMLBindingContext.class,true).setRelayState(relayState);
        }
        SAMLEndpointContext samlEndpointContext = messageContext.getSubcontext(SAMLPeerEntityContext.class,true).getSubcontext(SAMLEndpointContext.class,true);
        Endpoint endpoint = openSamlImplementation.buildSAMLObject(AssertionConsumerService.class);
        endpoint.setLocation(acsUrl);
        samlEndpointContext.setEndpoint(endpoint);
        //openSamlImplementation.
        HTTPPostEncoder httpPostEncoder = new HTTPPostEncoder();
        httpPostEncoder.setMessageContext(messageContext);
        httpPostEncoder.setVelocityEngine(velocityEngine);
        httpPostEncoder.setVelocityTemplateId(TEMPLATE_PATH);
        httpPostEncoder.setHttpServletResponse(res);
        try {
            httpPostEncoder.initialize();
        } catch (ComponentInitializationException e) {
            e.printStackTrace();
            return false;
        }
        try {
            httpPostEncoder.encode();
        } catch (MessageEncodingException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Velocity 引擎
     */
    private VelocityEngine velocityEngine;

    public SamlServiceImpl() {
        velocityEngine = new VelocityEngine();
        velocityEngine.setProperty(RuntimeConstants.ENCODING_DEFAULT,
                "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.OUTPUT_ENCODING,
                "UTF-8");
        velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER,
                "classpath");
        velocityEngine
                .setProperty("classpath.resource.loader.class",
                        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        try {
            velocityEngine.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
