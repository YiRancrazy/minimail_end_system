package com.yirancrazy.minimall.merchant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/*
 * 商家服务启动说明：加载 MyBatis Mapper 与跨服务 Feign 客户端，承载商家资质和店铺相关接口。
 */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家服务 Spring Boot 启动入口；负责扫描商家模块 Mapper、启用 Feign 客户端，并作为商家资质、店铺管理与内部查询接口的运行容器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.merchant.mapper")
@EnableFeignClients(basePackages = "com.yirancrazy.minimall.api.feign")
public class MerchantApplication {
    /**
     * 启动商家服务进程并加载 Spring Boot 上下文。
     *
     * @param args 命令行参数，本入口未做额外解析。
     */
    public static void main(String[] args) {
        SpringApplication.run(MerchantApplication.class, args);
    }
}
