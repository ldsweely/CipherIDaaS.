package cipher.console.oidc.service.impl;

import cipher.console.oidc.domain.web.SystemConfigInfo;
import cipher.console.oidc.mapper.SystemConfigInfoMapper;
import cipher.console.oidc.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * TODO:
 * create by liuying at 2019/9/9
 *
 * @author liuying
 * @since 2019/9/9 16:35
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private SystemConfigInfoMapper systemConfigInfoMapper;

    @Override
    public SystemConfigInfo getSystemConfigInfo(String companyUuid) {
        return systemConfigInfoMapper.getSystemConfigInfo(companyUuid);
    }
}
