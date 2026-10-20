-- Service-owned schema migration for notify.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_notify_message (
                                                id              BIGINT          NOT NULL COMMENT '主键ID',
                                                user_id         BIGINT          NOT NULL COMMENT '接收用户ID',
                                                recipient_type  TINYINT         NOT NULL DEFAULT 1 COMMENT '接收方类型：1=用户 2=商家 3=平台',
                                                message_type    TINYINT         NOT NULL DEFAULT 1 COMMENT '消息类型：1=订单 2=物流 3=退款 4=营销 5=公告 6=系统 7=违规',
                                                sender_id       BIGINT          DEFAULT NULL COMMENT '发送者ID，系统消息为NULL',
                                                biz_id          VARCHAR(64)     DEFAULT NULL COMMENT '业务关联ID，如订单号',
    title           VARCHAR(255)    NULL COMMENT '消息标题',
    content         VARCHAR(1024)   NULL COMMENT '消息内容',
    read_flag       INT             DEFAULT 0 COMMENT '已读标志：0=未读 1=已读',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_notify_message_user (user_id),
    INDEX idx_recipient (recipient_type, user_id, read_flag),
    UNIQUE KEY uk_notify_biz (recipient_type, user_id, message_type, biz_id, is_deleted),
    INDEX idx_notify_message_biz (biz_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内消息表';

CREATE TABLE IF NOT EXISTS t_notify_complaint (
                                                  id                  BIGINT          NOT NULL COMMENT '主键ID',
                                                  complainant_type    TINYINT         NOT NULL COMMENT '投诉方类型：1=用户 2=商家',
                                                  complainant_id      BIGINT          NOT NULL COMMENT '投诉方ID',
                                                  defendant_type      TINYINT         NOT NULL COMMENT '被诉方类型：1=用户 2=商家',
                                                  defendant_id        BIGINT          NOT NULL COMMENT '被诉方ID',
                                                  order_no            VARCHAR(64)     NULL COMMENT '关联订单号',
    complaint_type      VARCHAR(32)     NOT NULL COMMENT '投诉类型（编码）',
    title               VARCHAR(128)    NOT NULL COMMENT '投诉标题',
    content             TEXT            NOT NULL COMMENT '投诉内容',
    status              TINYINT         DEFAULT 0 COMMENT '状态：0=待处理 1=处理中 2=已解决 3=驳回',
    handler_id          BIGINT          NULL COMMENT '处理人ID',
    handler_result      TEXT            NULL COMMENT '处理结果',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_complainant (complainant_type, complainant_id),
    INDEX idx_notify_complaint_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='投诉表';

CREATE TABLE IF NOT EXISTS t_notify_comment (
                                                id                      BIGINT          NOT NULL COMMENT '主键ID',
                                                order_no                VARCHAR(64)     NOT NULL COMMENT '关联订单号',
    spu_id                  BIGINT          NOT NULL COMMENT '商品SPU ID',
    sku_id                  BIGINT          NULL COMMENT '商品SKU ID（可空）',
    user_id                 BIGINT          NOT NULL COMMENT '评价用户ID',
    merchant_id             BIGINT          NOT NULL COMMENT '商家ID，便于按商家查询',
    rating                  TINYINT         NOT NULL COMMENT '评分：1-5',
    content                 TEXT            NOT NULL COMMENT '评价内容',
    images                  TEXT            NULL COMMENT '评价图片，逗号分隔的 objectKey 列表',
    anonymous               TINYINT         DEFAULT 0 COMMENT '是否匿名：0=否 1=是',
    status                  TINYINT         DEFAULT 1 COMMENT '状态：1=正常 2=隐藏 3=已删除',
    merchant_reply          VARCHAR(1024)   NULL COMMENT '商家回复内容',
    merchant_reply_time     DATETIME        NULL COMMENT '商家回复时间',
    is_deleted              TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_spu (spu_id, status),
    INDEX idx_notify_comment_user (user_id),
    INDEX idx_merchant (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';

CREATE TABLE IF NOT EXISTS t_notify_preference (
                                                   id              BIGINT          NOT NULL COMMENT '主键ID',
                                                   user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                                   category_code   VARCHAR(32)     NOT NULL COMMENT '通知类别（见 NotifyCategoryEnum）',
    site_enabled    TINYINT         DEFAULT 1 COMMENT '站内信开关：0=关 1=开',
    sms_enabled     TINYINT         DEFAULT 1 COMMENT '短信开关：0=关 1=开',
    email_enabled   TINYINT         DEFAULT 1 COMMENT '邮件开关：0=关 1=开',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_category (user_id, category_code, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知偏好设置表';


SET FOREIGN_KEY_CHECKS = 1;
