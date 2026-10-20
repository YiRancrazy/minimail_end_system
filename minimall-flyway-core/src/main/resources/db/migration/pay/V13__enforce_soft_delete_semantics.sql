ALTER TABLE t_pay_transaction
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_pay_transaction_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_pay_refund
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_pay_refund_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
