package com.campus.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 配置属性（与 user-service 共享同一密钥）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hm.jwt")
public class JwtProperties {
    /**
     * 签名密钥（HS256 对称密钥）
     */
    private String secret;
    /**
     * token 有效期
     */
    private Duration tokenTTL;
}
