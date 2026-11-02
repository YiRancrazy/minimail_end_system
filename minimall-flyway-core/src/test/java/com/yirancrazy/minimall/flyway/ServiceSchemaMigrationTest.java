package com.yirancrazy.minimall.flyway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 服务级数据库迁移隔离集成测试.
 * @Version: 1.0
 * @DateTime: 2026/10/20
 */
@Testcontainers(disabledWithoutDocker = true)
public class ServiceSchemaMigrationTest {

    private static final String ROOT_SCHEMA = "bootstrap";
    private static final Set<String> UNDO_LOG_SCHEMAS =
            Set.of("auth", "user", "order", "stock", "pay");
    private static final Map<String, Set<String>> EXPECTED_TABLES =
            Map.of(
                    "auth",
                            Set.of(
                                    "t_auth_role",
                                    "t_auth_permission",
                                    "t_auth_user",
                                    "t_auth_role_permission",
                                    "t_auth_token_blacklist"),
                    "user", Set.of("t_user", "t_user_address", "t_user_favorite"),
                    "merchant", Set.of("t_merch_merchant", "t_merch_shop"),
                    "goods", Set.of("t_goods_spu", "t_sku", "t_goods_audit_record"),
                    "cart", Set.of("t_cart_item"),
                    "order",
                            Set.of(
                                    "t_order",
                                    "t_order_item",
                                    "t_order_logistics",
                                    "t_order_status_log",
                                    "t_order_payment_snapshot",
                                    "t_order_outbox",
                                    "t_order_cart_cleanup_task"),
                    "stock", Set.of("t_stock", "t_stock_journal", "t_stock_transfer", "t_stock_count_task"),
                    "pay", Set.of("t_pay_transaction", "t_pay_refund"),
                    "notify",
                            Set.of(
                                    "t_notify_message",
                                    "t_notify_complaint",
                                    "t_notify_comment",
                                    "t_notify_preference"),
                    "id", Set.of("t_id_segment"));

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0.33")
                    .withDatabaseName(ROOT_SCHEMA)
                    .withUsername("test")
                    .withPassword("test");

    /** 验证每个Schema使用仅归属迁移. */
    @Test
    void testEachSchemaUsesOnlyOwnedMigrations() throws SQLException {
        try (Connection root =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
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

    /** 验证库存流水释放索引拒绝重复业务键. */
    @Test
    void testStockJournalReleaseIndexRejectsDuplicateBusinessKey() throws SQLException {
        try (Connection root =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            createSchemas(root);
            migrate("stock");
            try (Connection stock =
                    DriverManager.getConnection(
                            schemaUrl("stock"), MYSQL.getUsername(), MYSQL.getPassword())) {
                assertEquals(
                        List.of("sku_id", "order_no", "type", "is_deleted"),
                        readIndexColumns(stock, "stock_db", "uk_stock_journal_release_idem"));

                insertReleaseJournal(stock, 100L, "OD100");
                SQLException exception =
                        assertThrows(SQLException.class, () -> insertReleaseJournal(stock, 100L, "OD100"));

                assertEquals("23000", exception.getSQLState());
                assertEquals(1, countReleaseJournal(stock, 100L, "OD100"));
            }
        }
    }

    /** 验证库存流水释放索引拒绝并发重复业务键. */
    @Test
    @Timeout(10)
    void testStockJournalReleaseIndexRejectsConcurrentDuplicateBusinessKey() throws Exception {
        try (Connection root =
                DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            createSchemas(root);
            migrate("stock");
        }

        ExecutorService executor =
                new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(1),
                        runnable -> {
                            Thread thread = new Thread(runnable, "stock-release-index-test");
                            thread.setDaemon(true);
                            return thread;
                        });
        try (Connection first =
                        DriverManager.getConnection(
                                schemaUrl("stock"), MYSQL.getUsername(), MYSQL.getPassword());
                Connection second =
                        DriverManager.getConnection(
                                schemaUrl("stock"), MYSQL.getUsername(), MYSQL.getPassword())) {
            first.setAutoCommit(false);
            insertReleaseJournal(first, 101L, "OD101");

            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<SQLException> duplicateResult =
                    executor.submit(
                            () -> {
                                secondStarted.countDown();
                                try {
                                    insertReleaseJournal(second, 101L, "OD101");
                                    return null;
                                } catch (SQLException exception) {
                                    return exception;
                                }
                            });

            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            first.commit();

            SQLException exception = duplicateResult.get(5, TimeUnit.SECONDS);
            assertNotNull(exception);
            assertEquals("23000", exception.getSQLState());
        } finally {
            executor.shutdownNow();
        }
    }

    /** * 验证投诉幂等键索引拒绝重复业务键. */
    @Test
    void testComplaintIdempotencyIndexRejectsDuplicateKey() throws SQLException {
        try (Connection root =
                DriverManager.getConnection(
                        MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            createSchemas(root);
            migrate("notify");
        }
        try (Connection notify =
                DriverManager.getConnection(
                        schemaUrl("notify"), MYSQL.getUsername(), MYSQL.getPassword())) {
            assertEquals(
                    List.of("idempotency_key", "is_deleted"),
                    readIndexColumns(
                            notify,
                            "notify_db",
                            "t_notify_complaint",
                            "uk_notify_complaint_idem"));
            insertComplaint(notify, 1001L, "complaint:create:100:req-1");
            SQLException exception =
                    assertThrows(
                            SQLException.class,
                            () ->
                                    insertComplaint(
                                            notify,
                                            1002L,
                                            "complaint:create:100:req-1"));
            assertEquals("23000", exception.getSQLState());
        }
    }

    private void createSchemas(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String domain : EXPECTED_TABLES.keySet()) {
                statement.execute(
                        "CREATE DATABASE IF NOT EXISTS `"
                                + domain
                                + "_db` "
                                + "DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        }
    }

    private void migrate(String domain) {
        Flyway.configure()
                .dataSource(schemaUrl(domain), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration/" + domain)
                .load()
                .migrate();
    }

    private String schemaUrl(String domain) {
        return MYSQL.getJdbcUrl().replace("/" + ROOT_SCHEMA, "/" + domain + "_db");
    }

    private List<String> readIndexColumns(Connection connection, String schema, String indexName)
            throws SQLException {
        return readIndexColumns(connection, schema, "t_stock_journal", indexName);
    }

    private List<String> readIndexColumns(
            Connection connection, String schema, String tableName, String indexName)
            throws SQLException {
        List<String> columns = new ArrayList<>(4);
        String sql =
                "SELECT column_name FROM information_schema.statistics "
                        + "WHERE table_schema = ? AND table_name = ? "
                        + "AND index_name = ? ORDER BY seq_in_index";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            statement.setString(3, indexName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    columns.add(resultSet.getString(1));
                }
            }
        }
        return columns;
    }

    private void insertComplaint(
            Connection connection, Long id, String idempotencyKey) throws SQLException {
        String sql =
                "INSERT INTO t_notify_complaint "
                        + "(id, complainant_type, complainant_id, defendant_type, "
                        + "defendant_id, complaint_type, title, content, status, "
                        + "idempotency_key, is_deleted) "
                        + "VALUES (?, 1, 100, 2, 200, 'QUALITY', 'test', 'test', 0, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setString(2, idempotencyKey);
            statement.executeUpdate();
        }
    }

    private void insertReleaseJournal(Connection connection, Long skuId, String orderNo)
            throws SQLException {
        String sql =
                "INSERT INTO t_stock_journal "
                        + "(sku_id, quantity, type, order_no, is_deleted) VALUES (?, ?, ?, ?, 0)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, skuId);
            statement.setLong(2, 1L);
            statement.setInt(3, 2);
            statement.setString(4, orderNo);
            statement.executeUpdate();
        }
    }

    private int countReleaseJournal(Connection connection, Long skuId, String orderNo)
            throws SQLException {
        String sql =
                "SELECT COUNT(*) FROM t_stock_journal " + "WHERE sku_id = ? AND order_no = ? AND type = 2";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, skuId);
            statement.setString(2, orderNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private Set<String> readBusinessTables(Connection connection, String schema) throws SQLException {
        Set<String> tables = new HashSet<>();
        String sql =
                "SELECT table_name FROM information_schema.tables "
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
        String sql =
                "SELECT COUNT(*) FROM information_schema.tables "
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
