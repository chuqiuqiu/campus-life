package com.campus.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 鉴权白名单配置（对标黑马商城 AuthProperties）
 */
@Data
@Component
@ConfigurationProperties(prefix = "hm.auth")
public class AuthProperties {
    /**
     * 无需登录即可访问的路径列表（Ant 风格）
     */
    private List<String> excludePaths;
}
