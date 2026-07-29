package com.yirancrazy.minimall.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台服务 Spring Boot 启动入口，负责平台管理员、对账、公告、强制关单等能力（当前阶段以健康检查为主）。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
public class PlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}