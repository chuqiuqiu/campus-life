package com.campus.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * JSON 配置：Long/BigInteger 序列化为 String，避免前端 JS 精度丢失（对标黑马商城 JsonConfig）
 * <p>
 * 注意：使用 Jackson2ObjectMapperBuilderCustomizer 定制 Spring Boot 自动配置的 ObjectMapper，
 * 而不是注册新的 @Primary ObjectMapper —— 这样才能保留 Boot 自带的 JavaTimeModule（LocalDateTime 序列化）。
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class JsonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> {
            // long -> string
            jacksonObjectMapperBuilder.serializerByType(Long.class, ToStringSerializer.instance);
            jacksonObjectMapperBuilder.serializerByType(BigInteger.class, ToStringSerializer.instance);
        };
    }
}
