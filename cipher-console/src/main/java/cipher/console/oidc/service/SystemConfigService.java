package cipher.console.oidc.service;

import cipher.console.oidc.domain.web.SystemConfigInfo;

/**
 * TODO:
 * create by liuying at 2019/9/9
 *
 * @author liuying
 * @since 2019/9/9 16:34
 */
public interface SystemConfigService {

    public SystemConfigInfo getSystemConfigInfo(String companyUuid);

}
