package com.yirancrazy.minimall.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台服务启动类，初始化 Spring Boot 应用上下文
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@SpringBootApplication
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}