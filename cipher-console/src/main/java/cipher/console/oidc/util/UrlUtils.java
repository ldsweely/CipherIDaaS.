package cipher.console.oidc.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @Author: TK
 * @Date: 2019/7/1 20:22
 */
public final class UrlUtils {

    private static Logger logger = LoggerFactory.getLogger(UrlUtils.class);


    public static String getDynamicUrl(String applicationUrl) {
        //入参校验
        if (StringUtils.isEmpty(applicationUrl)) {
            return null;
        }
        String host = "";
        String finalUrl = "";
        try {
            //构建目标地址
            URL destinationUrl = new URL(applicationUrl);
            //获取目标地址的host
            host = destinationUrl.getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return host;
    }
}
