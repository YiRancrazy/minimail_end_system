package com.yirancrazy.minimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
* 订单服务 Spring Boot 启动入口，负责启用服务发现、Feign 客户端扫描及 MyBatis Mapper 扫描。
 */
@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
@MapperScan("com.yirancrazy.minimall.order.mapper")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}