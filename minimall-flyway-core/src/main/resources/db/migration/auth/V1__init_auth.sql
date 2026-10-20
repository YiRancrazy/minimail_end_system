-- Service-owned schema migration for auth.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_auth_role (
                                           id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                           role_code       VARCHAR(32)     NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(64)     NOT NULL COMMENT '角色名称',
    description     VARCHAR(255)    NULL COMMENT '角色描述',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_code (role_code, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_auth_permission (
                                                 id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                 permission_code VARCHAR(64)     NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128)    NOT NULL COMMENT '权限名称',
    resource_type   TINYINT         NULL COMMENT '资源类型：1=菜单 2=按钮 3=API',
    resource_path   VARCHAR(255)    NULL COMMENT '资源路径',
    description     VARCHAR(255)    NULL COMMENT '权限描述',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_permission_code (permission_code, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS t_auth_user (
                                           id              BIGINT          NOT NULL COMMENT '主键ID',
                                           account         VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '登录账号',
    phone_enc       VARBINARY(256)  NULL COMMENT '加密手机号（AES）',
    email_enc       VARBINARY(256)  NULL COMMENT '加密邮箱（AES）',
    password_hash   VARCHAR(255)    NOT NULL COMMENT '密码哈希值',
    salt            VARCHAR(64)     NOT NULL COMMENT '密码盐值',
    account_type    TINYINT         NOT NULL DEFAULT 1 COMMENT '账号类型：1=用户 2=商家 3=平台',
    role_id         BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '角色ID，关联t_auth_role',
    nickname        VARCHAR(64)     NULL COMMENT '昵称',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态（预留）',
    last_login_at   DATETIME(3)     NULL COMMENT '最后登录时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_user_account (account, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证用户表';

CREATE TABLE IF NOT EXISTS t_auth_role_permission (
                                                      id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                      role_id         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
                                                      permission_id   BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
                                                      version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                                      is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
                                                      create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                      update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                                      PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_perm (role_id, permission_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS t_auth_token_blacklist (
                                                      jti             CHAR(32)        NOT NULL COMMENT '令牌唯一标识（JWT ID）',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    expires_at      DATETIME(3)     NOT NULL COMMENT '令牌过期时间',
    revoked_at      DATETIME(3)     NOT NULL COMMENT '撤销时间',
    reason          VARCHAR(64)     NULL COMMENT '撤销原因',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (jti),
    KEY idx_token_bl_user (user_id),
    KEY idx_token_bl_expires (expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='令牌黑名单表';

INSERT IGNORE INTO t_auth_role (id, role_code, role_name, description) VALUES
  (1, 'USER',     '普通用户',     'C端普通用户'),
  (2, 'MERCHANT', '商家',         'B端商家'),
  (3, 'PLATFORM', '平台管理员',   '平台运营管理员');

SET FOREIGN_KEY_CHECKS = 1;
