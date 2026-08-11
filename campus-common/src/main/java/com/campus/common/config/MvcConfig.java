package com.campus.common.config;

import com.campus.common.interceptos.UserInfoInterce;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置：注册用户信息拦截器（对标黑马商城 MvcConfig）
 */
@Configuration
@ConditionalOnClass(HandlerInterceptor.class)
public class MvcConfig implements WebMvcConfigurer {

    @Bean
    public UserInfoInterce userInfoInterce() {
        return new UserInfoInterce();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有请求，解析网关透传的 user-info 请求头
        registry.addInterceptor(userInfoInterce()).addPathPatterns("/**");
    }
}
