-- Service-owned schema migration for pay.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_pay_transaction (
                                                 id                  BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                 payment_no          VARCHAR(32)     NOT NULL COMMENT '支付流水号（唯一）',
    trade_no            VARCHAR(64)     DEFAULT NULL COMMENT '第三方交易号',
    order_no            VARCHAR(32)     NOT NULL COMMENT '关联订单号',
    user_id             BIGINT UNSIGNED NOT NULL COMMENT '付款用户ID',
    merchant_id         BIGINT UNSIGNED NOT NULL COMMENT '收款商家ID',
    amount              DECIMAL(12,2)   NOT NULL COMMENT '支付金额(元)',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status              TINYINT         NOT NULL DEFAULT 1
        COMMENT '状态：1=PENDING 2=SUCCESS 3=FAILED 4=CLOSED 5=REFUNDING 6=REFUNDED 7=FROZEN',
    channel             TINYINT         NOT NULL DEFAULT 0 COMMENT '支付渠道：1=支付宝 2=微信',
    channel_response    TEXT            DEFAULT NULL COMMENT '第三方回调原始报文',
    paid_at             DATETIME(3)     DEFAULT NULL COMMENT '支付成功时间',
    expire_at           DATETIME(3)     DEFAULT NULL COMMENT '支付过期时间',
    idempotency_key     VARCHAR(64)     DEFAULT NULL COMMENT '幂等键',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_transaction_payment_no (payment_no, is_deleted),
    INDEX idx_pay_transaction_order_no (order_no),
    INDEX idx_pay_transaction_user_id (user_id),
    INDEX idx_pay_transaction_merchant_id (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';

CREATE TABLE IF NOT EXISTS t_pay_refund (
                                            id                  BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                            refund_no           VARCHAR(32)     NOT NULL COMMENT '退款流水号（唯一）',
    payment_no          VARCHAR(32)     NOT NULL COMMENT '关联支付流水号',
    refund_trade_no     VARCHAR(64)     DEFAULT NULL COMMENT '第三方退款交易号',
    amount              DECIMAL(12,2)   NOT NULL COMMENT '退款金额(元)',
    reason              VARCHAR(512)    DEFAULT NULL COMMENT '退款原因',
    status              TINYINT         NOT NULL DEFAULT 0
        COMMENT '状态：0=待退款 1=已退款 2=失败 3=已关闭',
    notified_at         DATETIME(3)     DEFAULT NULL COMMENT '退款通知时间',
    idempotency_key     VARCHAR(64)     DEFAULT NULL COMMENT '幂等键',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_refund_refund_no (refund_no, is_deleted),
    INDEX idx_pay_refund_payment_no (payment_no)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款流水表';


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
