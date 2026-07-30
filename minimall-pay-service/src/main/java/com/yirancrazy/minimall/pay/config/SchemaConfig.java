package com.yirancrazy.minimall.pay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/**
* 支付服务 dev/test profile 数据库初始化配置，启动时加载 schema.sql 完成建表与种子数据。
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
     * 应用启动后执行，执行 classpath 下的 schema.sql 完成 t_pay_record 等表结构初始化。
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