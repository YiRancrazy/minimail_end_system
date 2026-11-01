package com.yirancrazy.minimall.notify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/*
 * 通知服务进程入口；Spring 容器负责装配 Mapper、Feign 客户端、服务、网关和 SSE 组件。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 启动 minimall-notify-service，扫描通知模块 Mapper 并启用 API 模块 Feign 客户端，默认监听 8209 端口.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.notify.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class NotifyApplication {
    /**
     * 启动 Spring Boot 应用并进入通知服务运行进程.
     *
     * @param args Spring Boot 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
    }
}
