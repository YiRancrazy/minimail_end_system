-- V28: 修正支付表名与字段，t_pay_record → t_pay_transaction，字段对齐 PayTransactionPO

DROP TABLE IF EXISTS t_pay_record;

CREATE TABLE IF NOT EXISTS t_pay_transaction (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键',
    payment_no VARCHAR(32) NOT NULL COMMENT '支付流水号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '第三方交易号',
    order_no VARCHAR(32) NOT NULL COMMENT '关联订单号',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '付款用户ID',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '收款商家ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '支付金额(元)',
    currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付1已支付2关闭3失败',
    channel TINYINT NOT NULL DEFAULT 0 COMMENT '1支付宝2微信',
    channel_response TEXT DEFAULT NULL COMMENT '第三方回调原始报文',
    paid_at DATETIME(3) DEFAULT NULL COMMENT '支付成功时间',
    expire_at DATETIME(3) DEFAULT NULL COMMENT '支付过期时间',
    idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0未删除1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_transaction_payment_no (payment_no),
    INDEX idx_pay_transaction_order_no (order_no),
    INDEX idx_pay_transaction_user_id (user_id),
    INDEX idx_pay_transaction_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
