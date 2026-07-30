package com.yirancrazy.minimall.stock.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import lombok.extern.slf4j.Slf4j;

/**
* 库存服务数据库初始化配置，仅在 dev 与 test profile 下生效。作为
 *               CommandLineRunner 在应用启动后执行 classpath 下的 schema.sql 建表脚本，
 *               使本地开发与 H2 单测无需手工准备库表。
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
     * 应用启动后执行 classpath 下的 schema.sql 完成库表初始化；执行失败仅记录 warn 日志
     * 并跳过，不阻断应用启动。
     *
     * @param args 命令行启动参数，此处不使用
     */
    @Override
    public void run(String... args) {
        try {
            ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(),
                new ClassPathResource("schema.sql"));
            log.info("schema.sql applied");
        }
        catch (ScriptException e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}