-- Service-owned schema migration for user.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_user (
                                      id              BIGINT          NOT NULL COMMENT '主键ID',
                                      username        VARCHAR(64)     NOT NULL COMMENT '用户名（唯一）',
    nickname        VARCHAR(64)     NULL COMMENT '昵称',
    phone           VARCHAR(512)    NULL COMMENT '手机号，AES加密存储',
    email           VARCHAR(512)    NULL COMMENT '邮箱，AES加密存储',
    avatar          VARCHAR(512)    NULL COMMENT '头像URL',
    gender          TINYINT         DEFAULT 0 COMMENT '性别：0=未知 1=男 2=女',
    phone_masked    VARCHAR(32)     DEFAULT NULL COMMENT '手机号脱敏，用于模糊查询（如138****8001）',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, is_deleted),
    INDEX idx_user_phone_masked (phone_masked)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基本信息表';

CREATE TABLE IF NOT EXISTS t_user_address (
                                              id              BIGINT          NOT NULL COMMENT '主键ID',
                                              user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                              receiver_name   VARCHAR(512)    NOT NULL COMMENT '收货人姓名，AES加密存储',
    receiver_phone  VARCHAR(512)    NOT NULL COMMENT '收货人手机号，AES加密存储',
    province        VARCHAR(32)     NOT NULL COMMENT '省份',
    city            VARCHAR(32)     NOT NULL COMMENT '城市',
    district        VARCHAR(32)     NOT NULL COMMENT '区/县',
    detail_address  VARCHAR(512)    NOT NULL COMMENT '详细地址，AES加密存储',
    is_default      TINYINT         NULL DEFAULT NULL COMMENT '是否默认地址：1=是 NULL=否',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_address_default (user_id, is_default, is_deleted),
    CONSTRAINT chk_user_address_default_flag CHECK (is_default IS NULL OR is_default = 1)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址表';

CREATE TABLE IF NOT EXISTS t_user_favorite (
                                               id              BIGINT          NOT NULL COMMENT '主键ID',
                                               user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                               sku_id          BIGINT          NOT NULL COMMENT '商品SKU ID',
                                               version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                               is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
                                               create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                               PRIMARY KEY (id),
    UNIQUE KEY uk_user_favorite_user_sku (user_id, sku_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';

SET FOREIGN_KEY_CHECKS = 1;
