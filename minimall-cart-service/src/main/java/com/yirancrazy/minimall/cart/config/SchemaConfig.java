package com.yirancrazy.minimall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车服务 dev/test profile 数据库初始化配置，应用启动时执行 classpath 下的 schema.sql。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Slf4j
@Configuration
@Profile({"test", "dev"})
public class SchemaConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Spring Boot 启动后执行 classpath 下的 schema.sql 完成数据库表结构初始化，失败时仅告警不中断启动。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        try {
            ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(),
                new ClassPathResource("schema.sql"));
            log.info("schema.sql applied");
        } catch (Exception e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}