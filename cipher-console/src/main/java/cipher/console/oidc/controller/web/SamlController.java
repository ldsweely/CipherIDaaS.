package cipher.console.oidc.controller.web;


import cipher.console.oidc.domain.web.ApplicationInfo;
import cipher.console.oidc.domain.web.ApplicationInfoDomain;
import cipher.console.oidc.domain.web.SystemConfigInfo;
import cipher.console.oidc.service.AppService;
import cipher.console.oidc.service.SessionService;
import cipher.console.oidc.service.SystemConfigService;
import cipher.console.oidc.util.OpenSamlImplementation;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.*;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.SignatureException;
import java.util.Date;

import static org.opensaml.saml.common.xml.SAMLConstants.SAML2_POST_BINDING_URI;

/**
 * TODO:
 * create by shizhao at 2019-12-11
 *
 * @author: shizhao
 * @since: 2019-12-11 14:37
 */
@RequestMapping(value = "/cipher/saml")
@Controller
public class SamlController {

   /* @Value("${spring.saml.ssourl}")
    private String ssourl;*/


    @Autowired
    private AppService appService;

    @Autowired
    private SessionService sessionService;
    /**
     * 不需要修改的字段
     */
    @Resource
    private OpenSamlImplementation openSamlImplementation;


    @Autowired
    private SystemConfigService systemConfigService;


    /**
     * 注意, 生成元数据的时候,使 EntityID 字段与 Issuer 里面的值一致,否则会有问题!
     * @return
     * @throws MarshallingException
     * @throws SignatureException
     * @throws SecurityException
     */
    @RequestMapping("/metadata")
    @ResponseBody
    public ResponseEntity<ByteArrayResource> metadata(String appId,HttpServletRequest request)
            throws MarshallingException, SignatureException, SecurityException, org.opensaml.xmlsec.signature.support.SignatureException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + System.currentTimeMillis() + ".xml");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        headers.add("Last-Modified", new Date().toString());
        headers.add("ETag", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new ByteArrayResource(this.generateIDPMetadataXML(appId,request).getBytes()));


    }

    public String generateIDPMetadataXML(String appId, HttpServletRequest request) throws MarshallingException, org.opensaml.xmlsec.signature.support.SignatureException, SecurityException {
        String companyId = sessionService.getCompanyUUid(request.getSession());
        EntityDescriptor entityDescriptor = openSamlImplementation.buildSAMLObject(EntityDescriptor.class);
        ApplicationInfoDomain applicationInfoDomain=new ApplicationInfoDomain();
        applicationInfoDomain.setId(Integer.valueOf(appId));
        applicationInfoDomain= appService.findApplicationById(applicationInfoDomain);
        //EntityId是metadata地址
        String idpEntityId =applicationInfoDomain.getEntityId();
        entityDescriptor.setEntityID(idpEntityId);
        //IDP用于SSO的描述符
        IDPSSODescriptor idpssoDescriptor = openSamlImplementation.buildSAMLObject(IDPSSODescriptor.class);
        //必须的
        idpssoDescriptor.addSupportedProtocol(SAMLConstants.SAML20P_NS);
        //不加签
        idpssoDescriptor.setWantAuthnRequestsSigned(false);
        //用于验断言的 Key 信息生成
        KeyDescriptor keyDescriptor = openSamlImplementation.buildSAMLObject(KeyDescriptor.class);
        KeyInfoGenerator keyInfoGenerator = openSamlImplementation.getKeyInfoGenerator(openSamlImplementation.getSelfCredential(applicationInfoDomain));
        keyDescriptor.setUse(UsageType.SIGNING);
        keyDescriptor.setKeyInfo(keyInfoGenerator.generate(openSamlImplementation.getSelfCredential(applicationInfoDomain)));
        idpssoDescriptor.getKeyDescriptors().add(keyDescriptor);
        //IDP返回的NameIDFormat
        NameIDFormat nameIDFormat = openSamlImplementation.buildSAMLObject(NameIDFormat.class);
        nameIDFormat.setFormat(NameIDType.UNSPECIFIED);
        idpssoDescriptor.getNameIDFormats().add(nameIDFormat);
        //SSO地址相关
        SingleSignOnService singleSignOnService = openSamlImplementation.buildSAMLObject(SingleSignOnService.class);
        singleSignOnService.setBinding(SAML2_POST_BINDING_URI);
        //本次接入这个URL不需要使用

        SystemConfigInfo systemConfigInfo=systemConfigService.getSystemConfigInfo(companyId);
        singleSignOnService.setLocation(systemConfigInfo.getPortalServerUrl() + "/cipher/saml/sso");

        idpssoDescriptor.getSingleSignOnServices().add(singleSignOnService);
        entityDescriptor.getRoleDescriptors().add(idpssoDescriptor);

        return openSamlImplementation.transformSAMLObject2String(entityDescriptor);
    }
}
