-- Service-owned schema migration for stock.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_stock (
                                       id              BIGINT          NOT NULL COMMENT '主键ID',
                                       sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
                                       merchant_id     BIGINT          NULL COMMENT '所属商家ID',
                                       available       BIGINT          DEFAULT 0 COMMENT '可用库存',
                                       reserved        BIGINT          DEFAULT 0 COMMENT '预占库存',
                                       alert_threshold BIGINT          NOT NULL DEFAULT 10 COMMENT '预警阈值',
                                       version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                       is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
                                       create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                       PRIMARY KEY (id),
    UNIQUE KEY uk_sku (sku_id, is_deleted),
    INDEX idx_stock_merchant (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存主表';

CREATE TABLE IF NOT EXISTS t_stock_journal (
                                               id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
                                               sku_id          BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
                                               quantity        BIGINT          NOT NULL COMMENT '变动数量，正=入 负=出',
                                               type            TINYINT         NOT NULL
        COMMENT '变动类型：1=预占 2=释放 3=调整 4=调出 5=调入 6=纠正',
                                               reason          VARCHAR(255)    NULL COMMENT '变动原因',
    order_no        VARCHAR(32)     NULL COMMENT '关联订单号',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存流水表';

CREATE TABLE IF NOT EXISTS t_stock_transfer (
                                                id              BIGINT          NOT NULL COMMENT '主键ID',
                                                from_sku_id     BIGINT          NOT NULL COMMENT '调出SKU ID',
                                                to_sku_id       BIGINT          NOT NULL COMMENT '调入SKU ID',
                                                quantity        BIGINT          NOT NULL COMMENT '调拨数量',
                                                reason          VARCHAR(256)    NULL COMMENT '调拨原因',
    operator_id     BIGINT          NOT NULL COMMENT '操作人（平台管理员ID）',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_from_sku (from_sku_id),
    INDEX idx_to_sku (to_sku_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调拨记录表';

CREATE TABLE IF NOT EXISTS t_stock_count_task (
                                                  id                  BIGINT          NOT NULL COMMENT '主键ID',
                                                  sku_id              BIGINT          NOT NULL COMMENT '盘点SKU ID',
                                                  expected_quantity   BIGINT          NOT NULL COMMENT '系统记录数量',
                                                  actual_quantity     BIGINT          NULL COMMENT '实际盘点数量（null=未盘点）',
                                                  diff_quantity       BIGINT          NULL COMMENT '差异数量（actual - expected）',
                                                  status              TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=待盘点 2=已完成 3=已取消',
                                                  remark              VARCHAR(256)    NULL COMMENT '备注',
    operator_id         BIGINT          NOT NULL COMMENT '操作人ID',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_sku (sku_id),
    INDEX idx_stock_count_task_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点任务表';

-- ============================================================
-- 框架表：Seata AT 模式 undo_log
-- ============================================================

CREATE TABLE IF NOT EXISTS `undo_log` (
                                          `branch_id`     BIGINT      NOT NULL COMMENT '分支事务ID',
                                          `xid`           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo log 上下文（如序列化方式）',
    `rollback_info` LONGBLOB    NOT NULL COMMENT '回滚信息',
    `log_status`    INT         NOT NULL COMMENT '状态：0=正常 1=防御',
    `log_created`   DATETIME(6) NOT NULL COMMENT '创建时间',
    `log_modified`  DATETIME(6) NOT NULL COMMENT '修改时间',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AT事务模式undo日志表';
SET FOREIGN_KEY_CHECKS = 1;
