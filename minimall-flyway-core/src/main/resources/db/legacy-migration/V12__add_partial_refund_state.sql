ALTER TABLE t_order
    ADD COLUMN total_refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT '累计已退款金额（元）' AFTER refund_amount;
