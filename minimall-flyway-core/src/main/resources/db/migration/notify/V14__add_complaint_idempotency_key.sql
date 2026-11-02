ALTER TABLE t_notify_complaint
    ADD COLUMN idempotency_key VARCHAR(96) NULL COMMENT '幂等键（唯一）' AFTER handler_result,
    ADD UNIQUE KEY uk_notify_complaint_idem (idempotency_key, is_deleted);
