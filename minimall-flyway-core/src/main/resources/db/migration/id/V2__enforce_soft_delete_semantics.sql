ALTER TABLE t_id_segment
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_id_segment_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
