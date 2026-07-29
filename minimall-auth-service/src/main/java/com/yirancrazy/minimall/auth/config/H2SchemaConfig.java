package com.yirancrazy.minimall.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;

/**
 * Iter-1 dev/test profile schema initializer. Runs schema.sql via JDBC at app
 * startup. Replaces Flyway/Liquibase (deferred to Iter-2).
 */
@Slf4j
@Configuration
@Profile({"test", "dev"})
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
        } catch (Exception e) {
            log.warn("schema.sql init skipped: {}", e.getMessage());
        }
    }
}