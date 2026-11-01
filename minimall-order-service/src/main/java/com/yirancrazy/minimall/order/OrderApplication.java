package com.yirancrazy.minimall.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * 应用启动入口.
 *
 * @param args args 参数
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务 Spring Boot 启动入口。
 *   业务边界：只负责加载应用上下文、Mapper、服务发现、Feign 和定时任务能力，不承载订单业务规则。
 *   使用场景：服务进程启动时由 JVM 调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.order.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
@EnableScheduling
public class OrderApplication {
    /**
     * 应用启动入口.
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
