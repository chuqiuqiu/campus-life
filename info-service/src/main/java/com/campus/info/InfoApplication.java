package com.campus.info;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 资讯失物服务启动类
 */
@MapperScan("com.campus.info.mapper")
@SpringBootApplication(scanBasePackages = {"com.campus.info", "com.campus.common"})
public class InfoApplication {
    public static void main(String[] args) {
        SpringApplication.run(InfoApplication.class, args);
    }
}
