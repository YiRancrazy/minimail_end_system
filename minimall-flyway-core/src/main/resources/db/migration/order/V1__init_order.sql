-- Service-owned schema migration for order.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_order (
                                       id                      BIGINT          NOT NULL COMMENT '主键ID',
                                       order_no                VARCHAR(32)     NOT NULL COMMENT '业务单号（唯一）',
    order_type              TINYINT         NOT NULL DEFAULT 1 COMMENT '订单类型：1=普通 2=拼团 3=秒杀（预留）',
    user_id                 BIGINT          NOT NULL COMMENT '下单用户ID',
    merchant_id             BIGINT          NULL COMMENT '商家ID',
    pay_id                  BIGINT          NULL COMMENT '支付流水ID（关联t_pay_transaction）',
    sku_id                  BIGINT          NULL COMMENT 'SKU ID（冗余，便于查询）',
    quantity                INT             NULL COMMENT '总数量（冗余）',
    amount                  DECIMAL(12,2)   DEFAULT 0 COMMENT '商品总额（原价）',
    refund_amount           DECIMAL(12,2)   NULL COMMENT '实际退款金额（元），部分退款时记录',
    total_amount            DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '应付总额（含运费）',
    pay_amount              DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '实付金额',
    freight_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '运费',
    discount_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '优惠金额',
    receiver_snapshot_json  TEXT            NULL COMMENT '下单收货信息快照（JSON）',
    pay_expire_at           DATETIME        NULL COMMENT '支付超时时间',
    paid_at                 DATETIME        NULL COMMENT '支付完成时间',
    received_at             DATETIME        NULL COMMENT '收货时间',
    shipped_at              DATETIME        NULL COMMENT '发货时间，用于自动确认收货判定',
    closed_at               DATETIME        NULL COMMENT '关闭时间',
    close_reason            VARCHAR(128)    NULL COMMENT '关闭原因',
    order_group_no          VARCHAR(32)     NULL COMMENT '订单组号，跨商家拆单时同一结算单的子订单共享',
    status                  TINYINT         NOT NULL DEFAULT 1
        COMMENT '状态：1=待支付 2=已支付 3=已发货 4=已完成 5=已取消 6=退款中 7=已退款',
    refund_from_status      TINYINT         NULL COMMENT '退款前状态，用于退款失败回退',
    client_ip               VARCHAR(45)     NULL COMMENT '客户端IP',
    idempotency_key         CHAR(36)        NULL COMMENT '幂等键（唯一）',
    version                 INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted              TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no, is_deleted),
    UNIQUE KEY uk_order_idem (idempotency_key, is_deleted),
    INDEX idx_order_user (user_id),
    INDEX idx_status (status),
    INDEX idx_order_group_no (order_group_no),
    INDEX idx_order_pay_expire (status, pay_expire_at),
    INDEX idx_order_user_status (user_id, status, create_time DESC),
    INDEX idx_order_merchant_status (merchant_id, status, create_time DESC)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单主表';

CREATE TABLE IF NOT EXISTS t_order_item (
                                            id                  BIGINT          NOT NULL COMMENT '主键ID',
                                            order_id            BIGINT          NOT NULL COMMENT '订单ID',
                                            spu_id              BIGINT          NULL COMMENT 'SPU ID',
                                            merchant_id         BIGINT          NULL COMMENT '商家ID',
                                            sku_id              BIGINT          NOT NULL COMMENT 'SKU ID',
                                            spu_snapshot_json   TEXT            NULL COMMENT '下单SPU快照（JSON）',
                                            sku_snapshot_json   TEXT            NULL COMMENT '下单SKU快照（JSON）',
                                            sku_name            VARCHAR(256)    NULL COMMENT 'SKU快照名称',
    sku_image_url       VARCHAR(255)    NULL COMMENT 'SKU图片',
    quantity            INT             NOT NULL COMMENT '购买数量',
    unit_price          DECIMAL(12,2)   NOT NULL COMMENT '下单时单价快照',
    amount              DECIMAL(12,2)   NOT NULL COMMENT '行金额 = unit_price * quantity',
    subtotal_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '行小计',
    discount_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '行优惠',
    pay_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '行实付',
    refund_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '退款状态：0=无 1=部分退款 2=全额退款',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_order_item_merchant (merchant_id),
    UNIQUE KEY uk_order_item_order_sku (order_id, sku_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单商品明细表';

CREATE TABLE IF NOT EXISTS t_order_logistics (
                                                 id              BIGINT          NOT NULL COMMENT '主键ID',
                                                 order_id        BIGINT          NOT NULL COMMENT '订单ID',
                                                 node            VARCHAR(64)     NOT NULL COMMENT '物流节点',
    description     VARCHAR(255)    NULL COMMENT '节点描述',
    created_time    DATETIME        NULL COMMENT '物流节点时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_order_id (order_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单物流轨迹表';

CREATE TABLE IF NOT EXISTS t_order_status_log (
                                                  id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                  order_id        BIGINT UNSIGNED NOT NULL COMMENT '关联订单ID',
                                                  from_status     TINYINT         DEFAULT NULL COMMENT '前一状态码',
                                                  to_status       TINYINT         NOT NULL COMMENT '新状态码',
                                                  trigger_source  VARCHAR(32)     NOT NULL COMMENT '触发来源：USER_PAY, MERCHANT_SHIP, SYSTEM_TIMEOUT 等',
    operator_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID，系统操作为NULL',
    note            VARCHAR(255)    DEFAULT NULL COMMENT '附加备注',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_order_status_log_order (order_id, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单状态转换日志表';

CREATE TABLE IF NOT EXISTS t_order_payment_snapshot (
                                                        id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                        order_id        BIGINT UNSIGNED NOT NULL COMMENT '关联订单ID（唯一）',
                                                        pay_amount      DECIMAL(12,2)   NOT NULL COMMENT '支付金额（人民币）',
    pay_method      TINYINT         NOT NULL COMMENT '支付方式：1=支付宝 2=微信',
    expire_at       DATETIME(3)     DEFAULT NULL COMMENT '支付过期时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_payment_snapshot_order (order_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单支付快照表（用于一致性校验）';

CREATE TABLE IF NOT EXISTS t_order_outbox (
                                              id              BIGINT          NOT NULL COMMENT '主键ID',
                                              transaction_id  VARCHAR(64)     NOT NULL COMMENT '事务ID',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单出站消息表（事务性发件箱）';


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
