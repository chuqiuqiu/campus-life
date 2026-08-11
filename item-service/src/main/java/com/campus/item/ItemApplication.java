package com.campus.item;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园闲置服务启动类
 */
@MapperScan("com.campus.item.mapper")
@EnableFeignClients(basePackages = "com.campus.item.client")
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.campus.item", "com.campus.common"})
public class ItemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItemApplication.class, args);
    }
}
