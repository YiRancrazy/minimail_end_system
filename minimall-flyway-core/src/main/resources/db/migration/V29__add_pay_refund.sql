-- V29: 新增退款表 t_pay_refund，对齐 PayRefundPO

CREATE TABLE IF NOT EXISTS t_pay_refund (
    id BIGINT UNSIGNED NOT NULL COMMENT '主键',
    refund_no VARCHAR(32) NOT NULL COMMENT '退款流水号',
    payment_no VARCHAR(32) NOT NULL COMMENT '关联支付流水号',
    refund_trade_no VARCHAR(64) DEFAULT NULL COMMENT '第三方退款交易号',
    amount DECIMAL(12,2) NOT NULL COMMENT '退款金额(元)',
    reason VARCHAR(512) DEFAULT NULL COMMENT '退款原因',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待退款1已退款2失败',
    notified_at DATETIME(3) DEFAULT NULL COMMENT '退款通知时间',
    idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键',
    create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '0未删除1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_refund_refund_no (refund_no),
    INDEX idx_pay_refund_payment_no (payment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水表';
