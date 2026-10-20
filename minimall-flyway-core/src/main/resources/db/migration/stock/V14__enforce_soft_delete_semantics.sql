ALTER TABLE t_stock
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_stock_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_stock_journal
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_stock_journal_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_stock_transfer
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_stock_transfer_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_stock_count_task
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_stock_count_task_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
