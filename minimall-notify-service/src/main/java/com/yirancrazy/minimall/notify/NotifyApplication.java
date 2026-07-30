package com.yirancrazy.minimall.notify;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
* 通知服务 Spring Boot 启动入口，负责注册至 Nacos 并扫描本服务与公共组件。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.notify.mapper")
public class NotifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
    }
}