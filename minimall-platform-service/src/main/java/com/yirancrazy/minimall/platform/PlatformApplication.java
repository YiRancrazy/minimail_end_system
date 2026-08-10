package com.yirancrazy.minimall.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.1
 * @DateTime: 2026/08/10
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}