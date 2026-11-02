ALTER TABLE t_order
    MODIFY COLUMN idempotency_key VARCHAR(96) NULL COMMENT '幂等键（唯一）';
