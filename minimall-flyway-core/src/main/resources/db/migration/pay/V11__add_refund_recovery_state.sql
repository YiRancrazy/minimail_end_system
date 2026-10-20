ALTER TABLE t_pay_refund
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '退款补偿重试次数' AFTER idempotency_key,
    ADD COLUMN next_retry_at DATETIME(3) NULL COMMENT '下次退款补偿时间' AFTER retry_count,
    ADD INDEX idx_pay_refund_status_retry (status, next_retry_at, is_deleted);
