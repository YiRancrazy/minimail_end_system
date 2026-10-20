ALTER TABLE t_order
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_order_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_order_item
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_order_item_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_order_logistics
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_order_logistics_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_order_status_log
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_order_status_log_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_order_payment_snapshot
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_order_payment_snapshot_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_order_outbox
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_order_outbox_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
