ALTER TABLE t_notify_message
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_notify_message_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_notify_complaint
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_notify_complaint_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_notify_comment
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_notify_comment_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_notify_preference
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_notify_preference_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
