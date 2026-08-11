package com.campus.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园活动服务启动类
 */
@MapperScan("com.campus.activity.mapper")
@EnableFeignClients(basePackages = "com.campus.activity.client")
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.campus.activity", "com.campus.common"})
public class ActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityApplication.class, args);
    }
}
