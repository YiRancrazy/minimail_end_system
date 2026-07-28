package com.yirancrazy.minimall.merchant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.yirancrazy.minimall")
@EnableDiscoveryClient
@MapperScan("com.yirancrazy.minimall.merchant.mapper")
public class MerchantApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantApplication.class, args);
    }
}