package com.campus.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * JWT 配置属性（对标黑马商城 JwtProperties）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hm.jwt")
public class JwtProperties {
    /**
     * 签名密钥（HS256 对称密钥，与网关保持一致）
     */
    private String secret;
    /**
     * token 有效期
     */
    private Duration tokenTTL;
}
