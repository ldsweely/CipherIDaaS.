package cipher.console.oidc.service.impl;

import cipher.console.oidc.mapper.ObtainCopyrightMapper;
import cipher.console.oidc.service.ObtainCopyrightService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ObtainCopyrightServiceImpl implements ObtainCopyrightService {

    @Autowired
    private ObtainCopyrightMapper obtainCopyrightMapper;

    @Override
    public String getCopyright(String companyUuid) {
        return obtainCopyrightMapper.getCopyright(companyUuid);
    }
}
