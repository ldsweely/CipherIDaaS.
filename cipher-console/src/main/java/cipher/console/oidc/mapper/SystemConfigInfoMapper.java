package cipher.console.oidc.mapper;


import cipher.console.oidc.domain.web.SystemConfigInfo;

/**
 * TODO:
 * create by liuying at 2019/8/20
 *
 * @author liuying
 * @since 2019/8/20 14:56
 */
public interface SystemConfigInfoMapper {

    public SystemConfigInfo getSystemConfigInfo(String companyUuid);
}
