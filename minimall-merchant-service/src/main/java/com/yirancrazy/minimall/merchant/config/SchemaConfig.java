package com.yirancrazy.minimall.merchant.config;

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
 * @Description: 数据库 Schema 配置类，初始化商户模块数据库表结构
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
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
        }
        catch (ScriptException | SQLException e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}