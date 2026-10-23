package com.yirancrazy.minimall.flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 服务级数据库迁移隔离集成测试。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
@Testcontainers(disabledWithoutDocker = true)
public class ServiceSchemaMigrationTest {

    private static final String ROOT_SCHEMA = "bootstrap";
    private static final Set<String> UNDO_LOG_SCHEMAS = Set.of(
        "auth", "user", "order", "stock", "pay");
    private static final Map<String, Set<String>> EXPECTED_TABLES = Map.of(
        "auth", Set.of("t_auth_role", "t_auth_permission", "t_auth_user",
            "t_auth_role_permission", "t_auth_token_blacklist"),
        "user", Set.of("t_user", "t_user_address", "t_user_favorite"),
        "merchant", Set.of("t_merch_merchant", "t_merch_shop"),
        "goods", Set.of("t_goods_spu", "t_sku", "t_goods_audit_record"),
        "cart", Set.of("t_cart_item"),
        "order", Set.of("t_order", "t_order_item", "t_order_logistics",
            "t_order_status_log", "t_order_payment_snapshot", "t_order_outbox"),
        "stock", Set.of("t_stock", "t_stock_journal", "t_stock_transfer", "t_stock_count_task"),
        "pay", Set.of("t_pay_transaction", "t_pay_refund"),
        "notify", Set.of("t_notify_message", "t_notify_complaint", "t_notify_comment",
            "t_notify_preference"),
        "id", Set.of("t_id_segment")
    );

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.33")
        .withDatabaseName(ROOT_SCHEMA)
        .withUsername("test")
        .withPassword("test");

    /**
     * 验证每个服务只加载本域迁移且只创建本域业务表。
     * @throws SQLException 数据库访问失败
     */
    @Test
    void eachSchemaUsesOnlyOwnedMigrations() throws SQLException {
        try (Connection root = DriverManager.getConnection(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            createSchemas(root);
            for (Map.Entry<String, Set<String>> entry : EXPECTED_TABLES.entrySet()) {
                migrate(entry.getKey());
                Set<String> actualTables = readBusinessTables(root, entry.getKey() + "_db");
                assertEquals(entry.getValue(), actualTables, entry.getKey());

                boolean undoLogExpected = UNDO_LOG_SCHEMAS.contains(entry.getKey());
                assertEquals(undoLogExpected, tableExists(root, entry.getKey() + "_db", "undo_log"));
                assertTrue(tableExists(root, entry.getKey() + "_db", "flyway_schema_history"));
            }
        }
    }

    private void createSchemas(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String domain : EXPECTED_TABLES.keySet()) {
                statement.execute("CREATE DATABASE `" + domain + "_db` "
                    + "DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        }
    }

    private void migrate(String domain) {
        String schemaUrl = MYSQL.getJdbcUrl().replace("/" + ROOT_SCHEMA, "/" + domain + "_db");
        Flyway.configure()
            .dataSource(schemaUrl, MYSQL.getUsername(), MYSQL.getPassword())
            .locations("classpath:db/migration/" + domain)
            .load()
            .migrate();
    }

    private Set<String> readBusinessTables(Connection connection, String schema) throws SQLException {
        Set<String> tables = new HashSet<>();
        String sql = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_type = 'BASE TABLE' "
            + "AND table_name NOT IN ('flyway_schema_history', 'undo_log')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString(1).toLowerCase());
                }
            }
        }
        return tables;
    }

    private boolean tableExists(Connection connection, String schema, String table)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.tables "
            + "WHERE table_schema = ? AND table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) == 1;
            }
        }
    }
}
