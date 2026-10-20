-- Service-owned schema migration for cart.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


CREATE TABLE IF NOT EXISTS t_cart_item (
                                           id              BIGINT          NOT NULL COMMENT '主键ID',
                                           user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                           sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
                                           quantity        INT             DEFAULT 1 COMMENT '数量',
                                           selected        INT             DEFAULT 1 COMMENT '是否选中：0=否 1=是',
                                           version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                           is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
                                           create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (id),
    UNIQUE KEY uk_user_sku (user_id, sku_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车项表';

SET FOREIGN_KEY_CHECKS = 1;
