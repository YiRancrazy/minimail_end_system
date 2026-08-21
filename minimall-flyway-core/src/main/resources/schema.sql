-- ============================================================
-- 薄荷商城（mini_mail_system）统一数据库初始化脚本
-- 由 V1-V49 共 49 个迁移文件合并而来（2026-08-18）
-- 等价于原 V1-V49 在空库上顺序执行的最终状态
-- 命名规范保持不变：t_xxx 表名 / uk_xxx 唯一索引 / idx_xxx 普通索引
-- 所有字段均已添加中文注释
-- 修改：所有 UNIQUE KEY 均增加 is_deleted 列（位于索引末尾）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 认证域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_auth_role (
                                           id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                           role_code       VARCHAR(32)     NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(64)     NOT NULL COMMENT '角色名称',
    description     VARCHAR(255)    NULL COMMENT '角色描述',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_code (role_code, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_auth_permission (
                                                 id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                 permission_code VARCHAR(64)     NOT NULL COMMENT '权限编码',
    permission_name VARCHAR(128)    NOT NULL COMMENT '权限名称',
    resource_type   TINYINT         NULL COMMENT '资源类型：1=菜单 2=按钮 3=API',
    resource_path   VARCHAR(255)    NULL COMMENT '资源路径',
    description     VARCHAR(255)    NULL COMMENT '权限描述',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=启用',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_permission_code (permission_code, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表';

CREATE TABLE IF NOT EXISTS t_auth_user (
                                           id              BIGINT          NOT NULL COMMENT '主键ID',
                                           account         VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '登录账号',
    phone_enc       VARBINARY(256)  NULL COMMENT '加密手机号（AES）',
    email_enc       VARBINARY(256)  NULL COMMENT '加密邮箱（AES）',
    password_hash   VARCHAR(255)    NOT NULL COMMENT '密码哈希值',
    salt            VARCHAR(64)     NOT NULL COMMENT '密码盐值',
    account_type    TINYINT         NOT NULL DEFAULT 1 COMMENT '账号类型：1=用户 2=商家 3=平台',
    role_id         BIGINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '角色ID，关联t_auth_role',
    nickname        VARCHAR(64)     NULL COMMENT '昵称',
    status          INT             NOT NULL DEFAULT 1 COMMENT '状态（预留）',
    last_login_at   DATETIME(3)     NULL COMMENT '最后登录时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_user_account (account, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证用户表';

CREATE TABLE IF NOT EXISTS t_auth_role_permission (
                                                      id              BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                      role_id         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
                                                      permission_id   BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
                                                      version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                                      is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
                                                      create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                      update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                                      PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_perm (role_id, permission_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS t_auth_token_blacklist (
                                                      jti             CHAR(32)        NOT NULL COMMENT '令牌唯一标识（JWT ID）',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    expires_at      DATETIME(3)     NOT NULL COMMENT '令牌过期时间',
    revoked_at      DATETIME(3)     NOT NULL COMMENT '撤销时间',
    reason          VARCHAR(64)     NULL COMMENT '撤销原因',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (jti),
    KEY idx_token_bl_user (user_id),
    KEY idx_token_bl_expires (expires_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='令牌黑名单表';

-- ============================================================
-- 用户域
-- ============================================================

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
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_default      TINYINT         DEFAULT 0 COMMENT '是否默认地址：0=否 1=是',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_user_address_default (user_id, is_default, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收货地址表';

CREATE TABLE IF NOT EXISTS t_user_favorite (
                                               id              BIGINT          NOT NULL COMMENT '主键ID',
                                               user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                               sku_id          BIGINT          NOT NULL COMMENT '商品SKU ID',
                                               version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                               is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
                                               create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                               PRIMARY KEY (id),
    UNIQUE KEY uk_user_favorite_user_sku (user_id, sku_id, is_deleted),
    INDEX idx_user_favorite_user_id (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表';

-- ============================================================
-- 商家域
-- ============================================================

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
    is_deleted              TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    status          VARCHAR(32)     DEFAULT 'PENDING' COMMENT '店铺状态：PENDING/ACTIVE/CLOSED',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_merch_shop_merchant (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='店铺表';

CREATE TABLE IF NOT EXISTS t_merchant_withdraw (
                                                   id              BIGINT          NOT NULL COMMENT '主键ID',
                                                   merchant_id     BIGINT          NOT NULL COMMENT '商家ID',
                                                   withdraw_no     VARCHAR(32)     NOT NULL COMMENT '提现单号',
    amount          DECIMAL(12,2)   NOT NULL COMMENT '提现金额（元）',
    status          TINYINT         NOT NULL COMMENT '状态：1=待审核 2=已通过 3=已拒绝 4=已打款',
    reason          VARCHAR(255)    NULL COMMENT '审核原因/备注',
    applied_at      DATETIME        NULL COMMENT '申请时间',
    reviewed_at     DATETIME        NULL COMMENT '审核时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_withdraw_no (withdraw_no, is_deleted),
    INDEX idx_merchant_id (merchant_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家提现记录表';

-- ============================================================
-- 商品域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_goods_spu (
                                           id              BIGINT          NOT NULL COMMENT '主键ID',
                                           spu_no          VARCHAR(32)     NOT NULL COMMENT 'SPU编号',
    merchant_id     BIGINT          NOT NULL COMMENT '商家ID',
    shop_id         BIGINT          NULL COMMENT '所属店铺ID',
    category_id     BIGINT          NOT NULL COMMENT '分类ID',
    title           VARCHAR(128)    NOT NULL COMMENT '商品标题',
    subtitle        VARCHAR(255)    NULL COMMENT '副标题',
    main_image_url  VARCHAR(255)    NULL COMMENT '主图URL',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '状态（0=待审核 1=上架 2=下架）',
    publish_at      DATETIME(3)     NULL COMMENT '上架时间',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_goods_spu_spu_no (spu_no, is_deleted),
    INDEX idx_goods_spu_merchant_status (merchant_id, status, update_time),
    INDEX idx_goods_spu_shop (shop_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SPU表';

CREATE TABLE IF NOT EXISTS t_sku (
                                     id              BIGINT          NOT NULL COMMENT '主键ID',
                                     spu_id          BIGINT          NOT NULL COMMENT '所属SPU ID',
                                     merchant_id     BIGINT          NULL COMMENT '所属商家ID',
                                     sku_name        VARCHAR(128)    NOT NULL COMMENT 'SKU名称（规格）',
    price           DECIMAL(12,2)   DEFAULT 0 COMMENT '单价（元）',
    stock           INT             DEFAULT 0 COMMENT '库存数量（冗余）',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_goods_audit_spu (spu_id, audit_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品审核记录表';

-- ============================================================
-- 交易域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_cart_item (
                                           id              BIGINT          NOT NULL COMMENT '主键ID',
                                           user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                           sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
                                           quantity        INT             DEFAULT 1 COMMENT '数量',
                                           selected        INT             DEFAULT 1 COMMENT '是否选中：0=否 1=是',
                                           version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                           is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
                                           create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                           PRIMARY KEY (id),
    INDEX idx_user (user_id),
    UNIQUE KEY uk_user_sku (user_id, sku_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车项表';

CREATE TABLE IF NOT EXISTS t_order (
                                       id                      BIGINT          NOT NULL COMMENT '主键ID',
                                       order_no                VARCHAR(32)     NULL COMMENT '业务单号（唯一）',
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
    status                  TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1=待支付 2=已支付 3=已发货 4=已收货 5=已取消 6=退款中 7=已退款',
    refund_from_status      TINYINT         NULL COMMENT '退款前状态，用于退款失败回退',
    client_ip               VARCHAR(45)     NULL COMMENT '客户端IP',
    idempotency_key         CHAR(36)        NULL COMMENT '幂等键（唯一）',
    version                 INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted              TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_order (order_id),
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
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_payment_snapshot_order (order_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单支付快照表（用于一致性校验）';

CREATE TABLE IF NOT EXISTS t_order_outbox (
                                              id              BIGINT          NOT NULL COMMENT '主键ID',
                                              transaction_id  VARCHAR(64)     NOT NULL COMMENT '事务ID',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transaction_id (transaction_id, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单出站消息表（事务性发件箱）';

-- ============================================================
-- 库存域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_stock (
                                       id              BIGINT          NOT NULL COMMENT '主键ID',
                                       sku_id          BIGINT          NOT NULL COMMENT 'SKU ID',
                                       merchant_id     BIGINT          NULL COMMENT '所属商家ID',
                                       available       BIGINT          DEFAULT 0 COMMENT '可用库存',
                                       reserved        BIGINT          DEFAULT 0 COMMENT '预占库存',
                                       alert_threshold BIGINT          NOT NULL DEFAULT 10 COMMENT '预警阈值',
                                       version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
                                       is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
                                               type            TINYINT         NOT NULL COMMENT '变动类型：1=预占 2=释放 3=调整',
                                               reason          VARCHAR(255)    NULL COMMENT '变动原因',
    order_no        VARCHAR(32)     NULL COMMENT '关联订单号',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_journal_sku_id (sku_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存流水表';

CREATE TABLE IF NOT EXISTS t_stock_transfer (
                                                id              BIGINT          NOT NULL COMMENT '主键ID',
                                                from_sku_id     BIGINT          NOT NULL COMMENT '调出SKU ID',
                                                to_sku_id       BIGINT          NOT NULL COMMENT '调入SKU ID',
                                                quantity        BIGINT          NOT NULL COMMENT '调拨数量',
                                                reason          VARCHAR(256)    NULL COMMENT '调拨原因',
    operator_id     BIGINT          NOT NULL COMMENT '操作人（平台管理员ID）',
    version         INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_sku (sku_id),
    INDEX idx_stock_count_task_status (status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点任务表';

-- ============================================================
-- 支付域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_pay_transaction (
                                                 id                  BIGINT UNSIGNED NOT NULL COMMENT '主键ID',
                                                 payment_no          VARCHAR(32)     NOT NULL COMMENT '支付流水号（唯一）',
    trade_no            VARCHAR(64)     DEFAULT NULL COMMENT '第三方交易号',
    order_no            VARCHAR(32)     NOT NULL COMMENT '关联订单号',
    user_id             BIGINT UNSIGNED NOT NULL COMMENT '付款用户ID',
    merchant_id         BIGINT UNSIGNED NOT NULL COMMENT '收款商家ID',
    amount              DECIMAL(12,2)   NOT NULL COMMENT '支付金额(元)',
    currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
    status              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0=待支付 1=已支付 2=关闭 3=失败',
    channel             TINYINT         NOT NULL DEFAULT 0 COMMENT '支付渠道：1=支付宝 2=微信',
    channel_response    TEXT            DEFAULT NULL COMMENT '第三方回调原始报文',
    paid_at             DATETIME(3)     DEFAULT NULL COMMENT '支付成功时间',
    expire_at           DATETIME(3)     DEFAULT NULL COMMENT '支付过期时间',
    idempotency_key     VARCHAR(64)     DEFAULT NULL COMMENT '幂等键',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    status              TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0=待退款 1=已退款 2=失败',
    notified_at         DATETIME(3)     DEFAULT NULL COMMENT '退款通知时间',
    idempotency_key     VARCHAR(64)     DEFAULT NULL COMMENT '幂等键',
    version             INT             NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_refund_refund_no (refund_no, is_deleted),
    INDEX idx_pay_refund_payment_no (payment_no)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款流水表';

-- ============================================================
-- 通知域
-- ============================================================

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
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_notify_message_user (user_id),
    INDEX idx_recipient (recipient_type, user_id, read_flag),
    UNIQUE KEY uk_notify_message_receiver_unread (recipient_type, user_id, read_flag, create_time DESC, is_deleted),
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
    is_deleted          TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
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
    is_deleted              TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_spu (spu_id, status),
    INDEX idx_notify_comment_user (user_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_notify_comment_order (order_no)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品评价表';

CREATE TABLE IF NOT EXISTS t_notify_preference (
                                                   id              BIGINT          NOT NULL COMMENT '主键ID',
                                                   user_id         BIGINT          NOT NULL COMMENT '用户ID',
                                                   category_code   VARCHAR(32)     NOT NULL COMMENT '通知类别（见 NotifyCategoryEnum）',
    site_enabled    TINYINT         DEFAULT 1 COMMENT '站内信开关：0=关 1=开',
    sms_enabled     TINYINT         DEFAULT 1 COMMENT '短信开关：0=关 1=开',
    email_enabled   TINYINT         DEFAULT 1 COMMENT '邮件开关：0=关 1=开',
    is_deleted      TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_category (user_id, category_code, is_deleted),
    INDEX idx_notify_preference_user (user_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知偏好设置表';

-- ============================================================
-- ID 域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_id_segment (
                                            id          BIGINT          NOT NULL COMMENT '主键ID',
                                            biz_tag     VARCHAR(64)     NOT NULL COMMENT '业务标识（如 order/pay/stock/user）',
    current_max BIGINT          DEFAULT 0 COMMENT '当前最大ID值',
    step        BIGINT          DEFAULT 1000 COMMENT '步长',
    version     INT             DEFAULT 0 COMMENT '乐观锁版本',
    is_deleted  TINYINT         DEFAULT 0 COMMENT '是否删除：0=未删除 1=已删除（允许NULL）',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_tag (biz_tag, is_deleted)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ID段分配表';

-- ============================================================
-- 种子数据
-- ============================================================

INSERT IGNORE INTO t_auth_role (id, role_code, role_name, description) VALUES
  (1, 'USER',     '普通用户',     'C端普通用户'),
  (2, 'MERCHANT', '商家',         'B端商家'),
  (3, 'PLATFORM', '平台管理员',   '平台运营管理员');

INSERT IGNORE INTO t_id_segment (id, biz_tag, current_max, step, version) VALUES
  (1, 'order', 10000, 1000, 0),
  (2, 'pay',   10000, 1000, 0),
  (3, 'stock', 10000, 1000, 0),
  (4, 'user',  10000, 1000, 0);

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