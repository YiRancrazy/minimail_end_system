package com.yirancrazy.minimall.notify.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
* 通知服务 dev/test profile 数据库初始化配置，启动时执行 schema.sql 建表脚本。
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
     * 启动时执行 classpath:schema.sql 初始化脚本，加载失败仅 warn 不阻断启动。
     *
     * @param args Spring Boot 启动参数
     */
    @Override
    public void run(String... args) {
        try {
            ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(),
                new ClassPathResource("schema.sql"));
            log.info("schema.sql applied");
        } catch (ScriptException e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}