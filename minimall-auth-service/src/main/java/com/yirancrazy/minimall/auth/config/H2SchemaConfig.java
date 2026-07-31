package com.yirancrazy.minimall.auth.config;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @Description: 数据库 Schema 配置类，初始化认证模块数据库表结构
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class H2SchemaConfig implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Spring {@link CommandLineRunner} callback that applies the bundled
     * {@code schema.sql} against the auto-configured H2 {@link DataSource} at
     * application startup for the {@code test}/{@code dev} profiles. Failures
     * are logged at WARN so a missing or malformed script does not abort boot.
     *
     * @param args the command-line arguments passed to the application; unused here
     */
    @Override
    public void run(String... args) {
        try {
            ScriptUtils.executeSqlScript(jdbcTemplate.getDataSource().getConnection(),
                new ClassPathResource("schema.sql"));
            log.info("schema.sql applied via H2SchemaConfig");
        }
        catch (ScriptException | SQLException e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}