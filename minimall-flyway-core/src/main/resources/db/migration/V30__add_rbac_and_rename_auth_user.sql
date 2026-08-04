-- V30__add_rbac_and_rename_auth_user.sql
-- 1. RBAC tables
CREATE TABLE IF NOT EXISTS t_auth_role (
    id              BIGINT UNSIGNED NOT NULL,
    role_code       VARCHAR(32)     NOT NULL,
    role_name       VARCHAR(64)     NOT NULL,
    description     VARCHAR(255)    NULL,
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_auth_permission (
    id              BIGINT UNSIGNED NOT NULL,
    permission_code VARCHAR(64)     NOT NULL,
    permission_name VARCHAR(128)    NOT NULL,
    resource_type   TINYINT         NULL COMMENT '1=menu 2=button 3=API',
    resource_path   VARCHAR(255)    NULL,
    description     VARCHAR(255)    NULL,
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_auth_role_permission (
    id              BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    permission_id   BIGINT UNSIGNED NOT NULL,
    is_deleted      TINYINT         NOT NULL DEFAULT 0,
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Token blacklist (DB as source of truth, Redis as cache)
CREATE TABLE IF NOT EXISTS t_auth_token_blacklist (
    jti             CHAR(32)        NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    expires_at      DATETIME(3)     NOT NULL,
    revoked_at      DATETIME(3)     NOT NULL,
    reason          VARCHAR(64)     NULL,
    PRIMARY KEY (jti),
    KEY idx_token_bl_user (user_id),
    KEY idx_token_bl_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Seed initial roles
INSERT INTO t_auth_role (id, role_code, role_name, description) VALUES
    (1, 'USER',     '普通用户',     'C端普通用户'),
    (2, 'MERCHANT', '商家',         'B端商家'),
    (3, 'PLATFORM', '平台管理员',   '平台运营管理员');

-- 4. Rename t_user_auth → t_auth_user and adjust columns
ALTER TABLE t_user_auth RENAME TO t_auth_user;

-- Add new columns
ALTER TABLE t_auth_user ADD COLUMN account      VARCHAR(64)     NOT NULL DEFAULT '' AFTER id;
ALTER TABLE t_auth_user ADD COLUMN phone_enc    VARBINARY(256)  NULL AFTER account;
ALTER TABLE t_auth_user ADD COLUMN email_enc    VARBINARY(256)  NULL AFTER phone_enc;
ALTER TABLE t_auth_user ADD COLUMN account_type TINYINT         NOT NULL DEFAULT 1  AFTER salt COMMENT '1=USER 2=MERCHANT 3=PLATFORM';
ALTER TABLE t_auth_user ADD COLUMN role_id      BIGINT UNSIGNED NOT NULL DEFAULT 1  AFTER account_type;
ALTER TABLE t_auth_user ADD COLUMN last_login_at DATETIME(3)    NULL AFTER status;

-- 5. Migrate data: username → account, role string → account_type + role_id
UPDATE t_auth_user SET account = username;
UPDATE t_auth_user SET account_type = CASE role
    WHEN 'USER' THEN 1
    WHEN 'MERCHANT' THEN 2
    WHEN 'PLATFORM' THEN 3
    ELSE 1 END;
UPDATE t_auth_user SET role_id = CASE role
    WHEN 'USER' THEN 1
    WHEN 'MERCHANT' THEN 2
    WHEN 'PLATFORM' THEN 3
    ELSE 1 END;

-- 6. Swap unique index and drop old columns
ALTER TABLE t_auth_user DROP INDEX uk_username;
ALTER TABLE t_auth_user ADD UNIQUE KEY uk_auth_user_account (account);
ALTER TABLE t_auth_user DROP COLUMN username;
ALTER TABLE t_auth_user DROP COLUMN role;
