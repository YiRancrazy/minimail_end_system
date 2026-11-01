package com.yirancrazy.minimall.goods;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * 启动阶段加载 Mapper、Feign 客户端与定时任务基础设施。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品服务启动入口，启用 Mapper 扫描、Feign 客户端和定时任务基础设施。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.goods.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
@EnableScheduling
public class GoodsApplication {
    /**
     * 启动商品服务并加载 Mapper、Feign 客户端和定时任务设施。
     *
     * @param args Spring Boot 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GoodsApplication.class, args);
    }
}
