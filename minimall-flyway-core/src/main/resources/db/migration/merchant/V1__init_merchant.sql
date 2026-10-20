-- Service-owned schema migration for merchant.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_merch_merchant (
                                                id                      BIGINT          NOT NULL COMMENT '主键ID',
                                                user_id                 BIGINT          NOT NULL COMMENT '关联用户ID',
                                                merchant_name           VARCHAR(64)     NOT NULL COMMENT '商家名称',
    license_no              VARCHAR(64)     NOT NULL COMMENT '营业执照号',
    audit_status            TINYINT         NOT NULL DEFAULT 0 COMMENT '审核状态（0=待审核 1=通过 2=驳回）',
    audit_reason            VARCHAR(255)    NULL COMMENT '审核原因/驳回说明',
    audit_at                DATETIME(3)     NULL COMMENT '审核时间',
    legal_person_enc        VARCHAR(512)    NULL COMMENT '法人姓名 AES-256-GCM 密文',
    legal_phone_enc         VARCHAR(512)    NULL COMMENT '法人手机 AES-256-GCM 密文',
    id_card_no_enc          VARCHAR(512)    NULL COMMENT '身份证号 AES-256-GCM 密文',
    business_license_no_enc VARCHAR(512)    NULL COMMENT '营业执照号 AES-256-GCM 密文',
    bank_account_enc        VARCHAR(512)    NULL COMMENT '提现银行卡 AES-256-GCM 密文',
    version                 INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted              TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_merch_merchant_user (user_id, is_deleted),
    INDEX idx_merch_status (audit_status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家主体表';

CREATE TABLE IF NOT EXISTS t_merch_shop (
                                            id              BIGINT          NOT NULL COMMENT '主键ID',
                                            merchant_id     BIGINT          NULL COMMENT '所属商家ID',
                                            shop_name       VARCHAR(128)    NOT NULL COMMENT '店铺名称',
    license_no      VARCHAR(64)     NULL COMMENT '店铺许可证号',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=营业中 2=已停业 3=已冻结',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_merch_shop_merchant (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺表';

SET FOREIGN_KEY_CHECKS = 1;
