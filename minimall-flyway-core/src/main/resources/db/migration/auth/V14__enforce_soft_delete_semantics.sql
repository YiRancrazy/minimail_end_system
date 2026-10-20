ALTER TABLE t_auth_role
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_auth_role_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_auth_permission
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_auth_permission_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_auth_user
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_auth_user_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_auth_role_permission
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_auth_role_permission_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);

ALTER TABLE t_auth_token_blacklist
    MODIFY COLUMN is_deleted TINYINT NULL DEFAULT 0
        COMMENT '是否删除：NULL=已删除 0=未删除',
    ADD CONSTRAINT chk_auth_token_blacklist_is_deleted
        CHECK (is_deleted IS NULL OR is_deleted = 0);
