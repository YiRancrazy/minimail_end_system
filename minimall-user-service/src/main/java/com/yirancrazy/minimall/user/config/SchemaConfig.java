package com.yirancrazy.minimall.user.config;

import java.sql.SQLException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 数据库 Schema 配置类，初始化用户模块数据库表结构
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class SchemaConfig implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 在 dev 或 test profile 启动时执行用户服务数据库 schema.sql 初始化脚本。
     */
    @Override
    public void run(String... args) {
        try {
            ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(),
                new ClassPathResource("schema.sql"));
            log.info("schema.sql applied");
        }
        catch (ScriptException | SQLException e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}