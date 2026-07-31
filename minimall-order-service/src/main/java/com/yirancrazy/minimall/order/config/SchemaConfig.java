package com.yirancrazy.minimall.order.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import lombok.extern.slf4j.Slf4j;

/**
* 订单服务开发与测试环境数据库初始化配置，应用启动时执行类路径下的 schema.sql 脚本。
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
     * 在应用启动后执行数据库建表脚本，初始化失败时记录警告并继续启动。
     *
     * @param args 应用启动参数
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
        catch (java.sql.SQLException e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}