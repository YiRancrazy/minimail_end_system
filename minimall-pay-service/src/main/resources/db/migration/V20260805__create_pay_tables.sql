-- V20260805__create_pay_tables.sql

-- Drop legacy table from early schema.sql
DROP TABLE IF EXISTS t_pay_record;

-- Create t_pay_transaction
CREATE TABLE t_pay_transaction (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    payment_no VARCHAR(32) NOT NULL UNIQUE COMMENT '商户单号',
    trade_no VARCHAR(64) COMMENT '支付宝单号',
    order_no VARCHAR(32) NOT NULL COMMENT '关联订单号',
    user_id BIGINT UNSIGNED NOT NULL,
    merchant_id BIGINT UNSIGNED NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) DEFAULT 'CNY',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付2成功3失败4关闭5退款中6已退款',
    channel TINYINT NOT NULL DEFAULT 1 COMMENT '1支付宝',
    channel_response JSON COMMENT '回调原始报文',
    paid_at DATETIME(3),
    expire_at DATETIME(3) NOT NULL,
    idempotency_key CHAR(36) UNIQUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_pay_order_no (order_no),
    INDEX idx_pay_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- Create t_pay_refund
CREATE TABLE t_pay_refund (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(32) NOT NULL UNIQUE,
    payment_no VARCHAR(32) NOT NULL,
    refund_trade_no VARCHAR(64),
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(255),
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发起1成功2失败3关闭',
    notified_at DATETIME(3),
    idempotency_key CHAR(36) UNIQUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_refund_payment_no (payment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款单表';
