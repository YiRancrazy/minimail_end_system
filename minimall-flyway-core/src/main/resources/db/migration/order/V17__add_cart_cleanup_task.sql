CREATE TABLE IF NOT EXISTS t_order_cart_cleanup_task (
    id             BIGINT       NOT NULL COMMENT '主键ID',
    order_group_no VARCHAR(32)  NOT NULL COMMENT '订单组号',
    user_id        BIGINT       NOT NULL COMMENT '用户ID',
    items_json     TEXT         NOT NULL COMMENT '清理项JSON',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0=待清理 1=已清理 2=失败',
    retry_count    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    next_retry_at  DATETIME     NOT NULL COMMENT '下次重试时间',
    last_error     VARCHAR(500) NULL COMMENT '最近失败原因',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted     TINYINT      DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_cleanup_group (order_group_no, is_deleted),
    INDEX idx_cart_cleanup_retry (status, next_retry_at, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='订单下单后购物车清理任务';
