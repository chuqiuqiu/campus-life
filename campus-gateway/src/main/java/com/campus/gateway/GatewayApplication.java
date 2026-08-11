package com.campus.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 统一网关启动类
 * 说明：gateway 为 WebFlux 应用，仅 import 使用 campus-common 的 R/UnauthorizedException 等普通类，
 * 不扫描 com.campus.common（其中的 servlet 相关组件如 CommonExceptionAdvice 在响应式环境下会报错）
 */
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
