package com.yirancrazy.minimall.merchant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
* 商家服务 dev/test profile 数据库初始化配置，启动时加载 schema.sql 完成表结构初始化。
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
     * 应用启动时执行，加载 classpath 下 schema.sql 完成店铺相关表结构初始化，异常时降级为警告日志。
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