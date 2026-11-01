package com.yirancrazy.minimall.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理服务启动入口，启用 Spring Boot 与 API 模块 Feign 客户端扫描.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class PlatformApplication {
    /**
     * 启动平台管理服务并加载平台端接口及远程服务客户端.
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
