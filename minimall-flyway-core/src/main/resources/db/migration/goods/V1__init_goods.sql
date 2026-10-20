-- Service-owned schema migration for goods.
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_goods_spu (
                                           id              BIGINT          NOT NULL COMMENT '主键ID',
                                           spu_no          VARCHAR(32)     NOT NULL COMMENT 'SPU编号',
    merchant_id     BIGINT          NOT NULL COMMENT '商家ID',
    category_id     BIGINT          NOT NULL COMMENT '分类ID',
    title           VARCHAR(128)    NOT NULL COMMENT '商品标题',
    subtitle        VARCHAR(255)    NULL COMMENT '副标题',
    main_image_url  VARCHAR(255)    NULL COMMENT '主图URL',
    status          TINYINT         NOT NULL DEFAULT 0
        COMMENT '状态：0=草稿 1=待审 2=在售 3=下架 4=审核驳回',
    publish_at      DATETIME(3)     NULL COMMENT '上架时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goods_spu_spu_no (spu_no, is_deleted),
    INDEX idx_goods_spu_merchant_status (merchant_id, status, update_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SPU表';

CREATE TABLE IF NOT EXISTS t_sku (
                                     id              BIGINT          NOT NULL COMMENT '主键ID',
                                     spu_id          BIGINT          NOT NULL COMMENT '所属SPU ID',
                                     merchant_id     BIGINT          NULL COMMENT '所属商家ID',
                                     sku_name        VARCHAR(128)    NOT NULL COMMENT 'SKU名称（规格）',
    price           DECIMAL(12,2)   DEFAULT 0 COMMENT '单价（元）',
    stock           INT             NOT NULL DEFAULT 0 COMMENT '库存展示缓存，权威库存见t_stock.available',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_goods_sku_spu (spu_id),
    INDEX idx_goods_sku_merchant (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SKU表';

CREATE TABLE IF NOT EXISTS t_goods_audit_record (
                                                    id              BIGINT          NOT NULL COMMENT '主键ID',
                                                    spu_id          BIGINT          NOT NULL COMMENT 'SPU ID',
                                                    auditor_id      BIGINT          NOT NULL COMMENT '审核人ID',
                                                    decision        TINYINT         NOT NULL COMMENT '审核决定：1=通过 2=驳回',
                                                    reason          VARCHAR(512)    NULL COMMENT '审核理由',
    audit_at        DATETIME(3)     NULL COMMENT '审核时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：NULL=已删除 0=未删除',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_goods_audit_spu (spu_id, audit_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品审核记录表';

SET FOREIGN_KEY_CHECKS = 1;
