ALTER TABLE t_auth_user
    ADD COLUMN user_id BIGINT NULL COMMENT '业务用户ID，关联 user_db.t_user.id' AFTER id,
    ADD UNIQUE KEY uk_auth_user_user_id (user_id),
    DROP COLUMN phone_enc,
    DROP COLUMN email_enc,
    DROP COLUMN nickname,
    ADD CONSTRAINT chk_auth_user_profile_link
        CHECK (account_type <> 1 OR user_id IS NOT NULL);

CREATE TABLE IF NOT EXISTS undo_log (
    branch_id     BIGINT       NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT          NOT NULL COMMENT '0:normal 1:defense',
    log_created   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    log_modified  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT undo log';
