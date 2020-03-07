package com.portal.filter;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @Author: zt
 * 不拦截默认的静态资源
 * 添加登录拦截器
 * @Date: 2018/5/28 21:30
 */
//@Configuration
@SpringBootConfiguration
public class MvcConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
        super.addResourceHandlers(registry);
    }


    @Bean
    public LoginInterCeptor loginInterCeptor() {
        return new LoginInterCeptor();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(loginInterCeptor());
    }
}
