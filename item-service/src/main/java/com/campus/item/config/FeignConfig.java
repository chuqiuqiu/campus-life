package com.campus.item.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置：调用其他服务时自动携带内部服务令牌 X-Inner-Token
 * 用于访问 user-service 的 /users/credit/add 等内部接口（防外部越权调用）
 */
@Configuration
public class FeignConfig {

    @Value("${hm.inner.token}")
    private String innerToken;

    @Bean
    public RequestInterceptor innerTokenInterceptor() {
        return template -> template.header("X-Inner-Token", innerToken);
    }
}
