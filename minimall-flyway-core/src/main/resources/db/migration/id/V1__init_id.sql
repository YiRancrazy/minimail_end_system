-- Service-owned schema migration for id.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_id_segment (
                                            id          BIGINT          NOT NULL COMMENT '主键ID',
                                            biz_tag     VARCHAR(64)     NOT NULL COMMENT '业务标识（如 order/pay/stock/user）',
    current_max BIGINT          DEFAULT 0 COMMENT '当前最大ID值',
    step        BIGINT          DEFAULT 1000 COMMENT '步长',
    version     INT             DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted  TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_tag (biz_tag, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ID段分配表';


INSERT IGNORE INTO t_id_segment (id, biz_tag, current_max, step, version) VALUES
  (1, 'order', 10000, 1000, 0),
  (2, 'pay',   10000, 1000, 0),
  (3, 'stock', 10000, 1000, 0),
  (4, 'user',  10000, 1000, 0);

SET FOREIGN_KEY_CHECKS = 1;
