package com.yirancrazy.minimall.id;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


        /**
         * 应用启动入口。
         * @param args args 参数
         */
/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdApplication 服务启动类。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@SpringBootApplication
@MapperScan("com.yirancrazy.minimall.id.mapper")
public class IdApplication {
    /**
    * 应用启动入口。
    * @param args 启动参数
    */
    public static void main(String[] args) {
        SpringApplication.run(IdApplication.class, args);
    }
}