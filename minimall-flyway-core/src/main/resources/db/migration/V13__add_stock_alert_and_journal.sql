-- V13__add_stock_alert_and_journal.sql

ALTER TABLE t_stock ADD COLUMN alert_threshold BIGINT NOT NULL DEFAULT 10 COMMENT '预警阈值';

CREATE TABLE t_stock_journal (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    sku_id BIGINT UNSIGNED NOT NULL,
    quantity BIGINT NOT NULL COMMENT '变动数量，正=入负=出',
    type TINYINT NOT NULL COMMENT '1预占2释放3调整',
    reason VARCHAR(255),
    order_no VARCHAR(32),
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_journal_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';
