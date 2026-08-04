-- V33: Create t_order_status_log and t_order_payment_snapshot tables.
-- t_order_status_log: order state machine event log (event sourcing).
-- t_order_payment_snapshot: payment snapshot for consistency verification.

CREATE TABLE IF NOT EXISTS t_order_status_log (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    order_id BIGINT UNSIGNED NOT NULL COMMENT 'Associated order ID',
    from_status TINYINT DEFAULT NULL COMMENT 'Previous status code',
    to_status TINYINT NOT NULL COMMENT 'New status code',
    trigger_source VARCHAR(32) NOT NULL COMMENT 'Trigger source: USER_PAY, MERCHANT_SHIP, SYSTEM_TIMEOUT, etc.',
    operator_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'Operator user ID, NULL for system',
    note VARCHAR(255) DEFAULT NULL COMMENT 'Additional note',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0=not deleted 1=deleted',
    version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (id),
    INDEX idx_order_status_log_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Order status transition log';

CREATE TABLE IF NOT EXISTS t_order_payment_snapshot (
    id BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
    order_id BIGINT UNSIGNED NOT NULL COMMENT 'Associated order ID (unique)',
    pay_amount DECIMAL(12,2) NOT NULL COMMENT 'Payment amount in CNY',
    pay_method TINYINT NOT NULL COMMENT 'Payment method: 1=Alipay 2=WeChat',
    expire_at DATETIME(3) DEFAULT NULL COMMENT 'Payment expiration time',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0=not deleted 1=deleted',
    version INT NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_payment_snapshot_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Order payment snapshot for consistency verification';
