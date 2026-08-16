package com.yirancrazy.minimall.merchant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.1
 * @DateTime: 2026/08/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.merchant.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class MerchantApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantApplication.class, args);
    }
}