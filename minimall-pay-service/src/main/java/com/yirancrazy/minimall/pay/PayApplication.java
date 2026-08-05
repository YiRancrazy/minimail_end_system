package com.yirancrazy.minimall.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.pay.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
@EnableScheduling
public class PayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}