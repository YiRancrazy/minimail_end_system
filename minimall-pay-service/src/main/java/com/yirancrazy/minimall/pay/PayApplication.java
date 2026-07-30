package com.yirancrazy.minimall.pay;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
* 支付服务 Spring Boot 启动入口，负责注册 Nacos、扫描 Mapper 与初始化支付宝渠道上下文。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.pay.mapper")
public class PayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayApplication.class, args);
    }
}