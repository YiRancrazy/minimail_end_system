ALTER TABLE t_cart_item
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_cart_item_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
