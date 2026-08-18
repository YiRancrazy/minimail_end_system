-- ============================================================
-- 薄荷商城（mini_mail_system）统一数据库初始化脚本
-- 由 V1-V49 共 49 个迁移文件合并而来（2026-08-18）
-- 等价于原 V1-V49 在空库上顺序执行的最终状态
-- 命名规范保持不变：t_xxx 表名 / uk_xxx 唯一索引 / idx_xxx 普通索引
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 认证域（t_auth_role / t_auth_permission 先于被语义引用的表）
-- ============================================================

CREATE TABLE IF NOT EXISTS t_auth_role (
  id              BIGINT UNSIGNED NOT NULL,
  role_code       VARCHAR(32)     NOT NULL,
  role_name       VARCHAR(64)     NOT NULL,
  description     VARCHAR(255)    NULL,
  status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         NOT NULL DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_auth_permission (
  id              BIGINT UNSIGNED NOT NULL,
  permission_code VARCHAR(64)     NOT NULL,
  permission_name VARCHAR(128)    NOT NULL,
  resource_type   TINYINT         NULL COMMENT '1=menu 2=button 3=API',
  resource_path   VARCHAR(255)    NULL,
  description     VARCHAR(255)    NULL,
  status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=禁用 1=启用',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         NOT NULL DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_auth_user (
  id              BIGINT          NOT NULL,
  account         VARCHAR(64)     NOT NULL DEFAULT '',
  phone_enc       VARBINARY(256)  NULL,
  email_enc       VARBINARY(256)  NULL,
  password_hash   VARCHAR(255)    NOT NULL,
  salt            VARCHAR(64)     NOT NULL,
  account_type    TINYINT         NOT NULL DEFAULT 1 COMMENT '1=USER 2=MERCHANT 3=PLATFORM',
  role_id         BIGINT UNSIGNED NOT NULL DEFAULT 1,
  nickname        VARCHAR(64)     NULL COMMENT '昵称',
  status          INT             NOT NULL DEFAULT 1,
  last_login_at   DATETIME(3)     NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_user_account (account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_auth_role_permission (
  id              BIGINT UNSIGNED NOT NULL,
  role_id         BIGINT UNSIGNED NOT NULL,
  permission_id   BIGINT UNSIGNED NOT NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         NOT NULL DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_auth_token_blacklist (
  jti             CHAR(32)        NOT NULL,
  user_id         BIGINT UNSIGNED NOT NULL,
  expires_at      DATETIME(3)     NOT NULL,
  revoked_at      DATETIME(3)     NOT NULL,
  reason          VARCHAR(64)     NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  PRIMARY KEY (jti),
  KEY idx_token_bl_user (user_id),
  KEY idx_token_bl_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 用户域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_user (
  id              BIGINT          NOT NULL,
  username        VARCHAR(64)     NOT NULL,
  nickname        VARCHAR(64)     NULL,
  phone           VARCHAR(512)    NULL COMMENT '手机号，AES加密存储',
  email           VARCHAR(512)    NULL COMMENT '邮箱，AES加密存储',
  avatar          VARCHAR(512)    NULL COMMENT '头像URL',
  gender          TINYINT         DEFAULT 0 COMMENT '性别 0=未知 1=男 2=女',
  phone_masked    VARCHAR(32)     DEFAULT NULL COMMENT 'Masked phone for fuzzy query (e.g., 138****8001)',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  INDEX idx_user_phone_masked (phone_masked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_user_address (
  id              BIGINT          NOT NULL,
  user_id         BIGINT          NOT NULL COMMENT '用户ID',
  receiver_name   VARCHAR(512)    NOT NULL COMMENT '收货人姓名，AES加密存储',
  receiver_phone  VARCHAR(512)    NOT NULL COMMENT '收货人手机号，AES加密存储',
  province        VARCHAR(32)     NOT NULL COMMENT '省份',
  city            VARCHAR(32)     NOT NULL COMMENT '城市',
  district        VARCHAR(32)     NOT NULL COMMENT '区/县',
  detail_address  VARCHAR(512)    NOT NULL COMMENT '详细地址，AES加密存储',
  is_default      TINYINT         DEFAULT 0 COMMENT '是否默认 0=否 1=是',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_user_id (user_id),
  UNIQUE KEY uk_user_address_default (user_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_user_favorite (
  id              BIGINT          NOT NULL,
  user_id         BIGINT          NOT NULL,
  sku_id          BIGINT          NOT NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_favorite_user_sku (user_id, sku_id),
  INDEX idx_user_favorite_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 商家域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_merch_merchant (
  id                      BIGINT          NOT NULL,
  user_id                 BIGINT          NOT NULL,
  merchant_name           VARCHAR(64)     NOT NULL,
  license_no              VARCHAR(64)     NOT NULL,
  audit_status            TINYINT         NOT NULL DEFAULT 0,
  audit_reason            VARCHAR(255)    NULL,
  audit_at                DATETIME(3)     NULL,
  legal_person_enc        VARCHAR(512)    NULL COMMENT '法人姓名 AES-256-GCM 密文',
  legal_phone_enc         VARCHAR(512)    NULL COMMENT '法人手机 AES-256-GCM 密文',
  id_card_no_enc          VARCHAR(512)    NULL COMMENT '身份证号 AES-256-GCM 密文',
  business_license_no_enc VARCHAR(512)    NULL COMMENT '营业执照号 AES-256-GCM 密文',
  bank_account_enc        VARCHAR(512)    NULL COMMENT '提现银行卡 AES-256-GCM 密文',
  version                 INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted              TINYINT         DEFAULT 0,
  create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_merch_merchant_user (user_id),
  INDEX idx_merch_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_merch_shop (
  id              BIGINT          NOT NULL,
  merchant_id     BIGINT          NULL COMMENT '所属商家ID',
  shop_name       VARCHAR(128)    NOT NULL,
  license_no      VARCHAR(64)     NULL,
  status          VARCHAR(32)     DEFAULT 'PENDING',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_merch_shop_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_merchant_withdraw (
  id              BIGINT          NOT NULL,
  merchant_id     BIGINT          NOT NULL,
  withdraw_no     VARCHAR(32)     NOT NULL,
  amount          DECIMAL(12,2)   NOT NULL,
  status          TINYINT         NOT NULL COMMENT '1=待审核 2=已通过 3=已拒绝 4=已打款',
  reason          VARCHAR(255)    NULL,
  applied_at      DATETIME        NULL,
  reviewed_at     DATETIME        NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_withdraw_no (withdraw_no),
  INDEX idx_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 商品域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_goods_spu (
  id              BIGINT          NOT NULL,
  spu_no          VARCHAR(32)     NOT NULL,
  merchant_id     BIGINT          NOT NULL,
  category_id     BIGINT          NOT NULL,
  title           VARCHAR(128)    NOT NULL,
  subtitle        VARCHAR(255)    NULL,
  main_image_url  VARCHAR(255)    NULL,
  status          TINYINT         NOT NULL DEFAULT 0,
  publish_at      DATETIME(3)     NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_goods_spu_spu_no (spu_no),
  INDEX idx_goods_spu_merchant_status (merchant_id, status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_sku (
  id              BIGINT          NOT NULL,
  spu_id          BIGINT          NOT NULL,
  merchant_id     BIGINT          NULL COMMENT '所属商家ID',
  sku_name        VARCHAR(128)    NOT NULL,
  price           DECIMAL(12,2)   DEFAULT 0,
  stock           INT             DEFAULT 0,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_goods_sku_spu (spu_id),
  INDEX idx_goods_sku_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_goods_audit_record (
  id              BIGINT          NOT NULL,
  spu_id          BIGINT          NOT NULL,
  auditor_id      BIGINT          NOT NULL,
  decision        TINYINT         NOT NULL COMMENT '1=通过 2=驳回',
  reason          VARCHAR(512)    NULL,
  audit_at        DATETIME(3)     NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_goods_audit_spu (spu_id, audit_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 交易域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_cart_item (
  id              BIGINT          NOT NULL,
  user_id         BIGINT          NOT NULL,
  sku_id          BIGINT          NOT NULL,
  quantity        INT             DEFAULT 1,
  selected        INT             DEFAULT 1,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_user (user_id),
  UNIQUE KEY uk_user_sku (user_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_order (
  id                      BIGINT          NOT NULL,
  order_no                VARCHAR(32)     NULL COMMENT '业务单号',
  order_type              TINYINT         NOT NULL DEFAULT 1 COMMENT '1-普通 2-拼团 3-秒杀（预留）',
  user_id                 BIGINT          NOT NULL,
  merchant_id             BIGINT          NULL,
  pay_id                  BIGINT          NULL,
  sku_id                  BIGINT          NULL,
  quantity                INT             NULL,
  amount                  DECIMAL(12,2)   DEFAULT 0,
  refund_amount           DECIMAL(12,2)   NULL COMMENT '实际退款金额（元），部分退款时记录',
  total_amount            DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '应付总额',
  pay_amount              DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '实付金额',
  freight_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '运费',
  discount_amount         DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '优惠金额',
  receiver_snapshot_json  TEXT            NULL COMMENT '下单收货信息快照',
  pay_expire_at           DATETIME        NULL COMMENT '支付超时时间',
  paid_at                 DATETIME        NULL COMMENT '支付完成时间',
  received_at             DATETIME        NULL COMMENT '收货时间',
  shipped_at              DATETIME        NULL COMMENT '发货时间，用于自动确认收货判定',
  closed_at               DATETIME        NULL COMMENT '关闭时间',
  close_reason            VARCHAR(128)    NULL COMMENT '关闭原因',
  order_group_no          VARCHAR(32)     NULL COMMENT '订单组号，跨商家拆单时同一结算单的子订单共享',
  status                  TINYINT         NOT NULL DEFAULT 1 COMMENT '1待支付2已支付3已发货4已收货5已取消6退款中7已退款',
  refund_from_status      TINYINT         NULL COMMENT '退款前状态，用于退款失败回退',
  client_ip               VARCHAR(45)     NULL COMMENT '客户端IP',
  idempotency_key         CHAR(36)        NULL COMMENT '幂等键',
  version                 INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted              TINYINT         DEFAULT 0,
  create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_idem (idempotency_key),
  INDEX idx_order_user (user_id),
  INDEX idx_status (status),
  INDEX idx_order_group_no (order_group_no),
  INDEX idx_order_pay_expire (status, pay_expire_at),
  INDEX idx_order_user_status (user_id, status, create_time DESC),
  INDEX idx_order_merchant_status (merchant_id, status, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_order_item (
  id                  BIGINT          NOT NULL,
  order_id            BIGINT          NOT NULL,
  spu_id              BIGINT          NULL COMMENT 'SPU ID',
  merchant_id         BIGINT          NULL COMMENT '商家ID',
  sku_id              BIGINT          NOT NULL,
  spu_snapshot_json   TEXT            NULL COMMENT '下单SPU快照',
  sku_snapshot_json   TEXT            NULL COMMENT '下单SKU快照',
  sku_name            VARCHAR(256)    NULL COMMENT 'SKU快照名称',
  sku_image_url       VARCHAR(255)    NULL COMMENT 'SKU图片',
  quantity            INT             NOT NULL,
  unit_price          DECIMAL(12,2)   NOT NULL COMMENT '下单时单价快照',
  amount              DECIMAL(12,2)   NOT NULL COMMENT '行金额 = unit_price * quantity',
  subtotal_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '行小计',
  discount_amount     DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '行优惠',
  pay_amount          DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '行实付',
  refund_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '0-无 1-部分退款 2-全额退款',
  version             INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted          TINYINT         DEFAULT 0,
  create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_order (order_id),
  INDEX idx_order_item_merchant (merchant_id),
  UNIQUE KEY uk_order_item_order_sku (order_id, sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_order_logistics (
  id              BIGINT          NOT NULL,
  order_id        BIGINT          NOT NULL,
  node            VARCHAR(64)     NOT NULL,
  description     VARCHAR(255)    NULL,
  created_time    DATETIME        NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_order_status_log (
  id              BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
  order_id        BIGINT UNSIGNED NOT NULL COMMENT 'Associated order ID',
  from_status     TINYINT         DEFAULT NULL COMMENT 'Previous status code',
  to_status       TINYINT         NOT NULL COMMENT 'New status code',
  trigger_source  VARCHAR(32)     NOT NULL COMMENT 'Trigger source: USER_PAY, MERCHANT_SHIP, SYSTEM_TIMEOUT, etc.',
  operator_id     BIGINT UNSIGNED DEFAULT NULL COMMENT 'Operator user ID, NULL for system',
  note            VARCHAR(255)    DEFAULT NULL COMMENT 'Additional note',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=not deleted 1=deleted',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
  PRIMARY KEY (id),
  INDEX idx_order_status_log_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Order status transition log';

CREATE TABLE IF NOT EXISTS t_order_payment_snapshot (
  id              BIGINT UNSIGNED NOT NULL COMMENT 'Primary key',
  order_id        BIGINT UNSIGNED NOT NULL COMMENT 'Associated order ID (unique)',
  pay_amount      DECIMAL(12,2)   NOT NULL COMMENT 'Payment amount in CNY',
  pay_method      TINYINT         NOT NULL COMMENT 'Payment method: 1=Alipay 2=WeChat',
  expire_at       DATETIME(3)     DEFAULT NULL COMMENT 'Payment expiration time',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         NOT NULL DEFAULT 0 COMMENT '0=not deleted 1=deleted',
  create_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT 'Creation time',
  update_time     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT 'Update time',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_payment_snapshot_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Order payment snapshot for consistency verification';

CREATE TABLE IF NOT EXISTS t_order_outbox (
  id              BIGINT          NOT NULL,
  transaction_id  VARCHAR(64)     NOT NULL,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 库存域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_stock (
  id              BIGINT          NOT NULL,
  sku_id          BIGINT          NOT NULL,
  merchant_id     BIGINT          NULL COMMENT '所属商家ID',
  available       BIGINT          DEFAULT 0,
  reserved        BIGINT          DEFAULT 0,
  alert_threshold BIGINT          NOT NULL DEFAULT 10 COMMENT '预警阈值',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sku (sku_id),
  INDEX idx_stock_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_stock_journal (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sku_id          BIGINT UNSIGNED NOT NULL,
  quantity        BIGINT          NOT NULL COMMENT '变动数量，正=入负=出',
  type            TINYINT         NOT NULL COMMENT '1预占2释放3调整',
  reason          VARCHAR(255)    NULL,
  order_no        VARCHAR(32)     NULL,
  is_deleted      TINYINT         NOT NULL DEFAULT 0,
  created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  INDEX idx_journal_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存流水表';

CREATE TABLE IF NOT EXISTS t_stock_transfer (
  id              BIGINT          NOT NULL,
  from_sku_id     BIGINT          NOT NULL COMMENT '调出SKU',
  to_sku_id       BIGINT          NOT NULL COMMENT '调入SKU',
  quantity        BIGINT          NOT NULL COMMENT '调拨数量',
  reason          VARCHAR(256)    NULL COMMENT '调拨原因',
  operator_id     BIGINT          NOT NULL COMMENT '操作人（平台管理员）',
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_from_sku (from_sku_id),
  INDEX idx_to_sku (to_sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_stock_count_task (
  id                  BIGINT          NOT NULL,
  sku_id              BIGINT          NOT NULL COMMENT '盘点SKU',
  expected_quantity   BIGINT          NOT NULL COMMENT '系统记录数量',
  actual_quantity     BIGINT          NULL COMMENT '实际盘点数量（null=未盘点）',
  diff_quantity       BIGINT          NULL COMMENT '差异数量（actual - expected）',
  status              TINYINT         NOT NULL DEFAULT 1 COMMENT '1=PENDING 2=COMPLETED 3=CANCELLED',
  remark              VARCHAR(256)    NULL COMMENT '备注',
  operator_id         BIGINT          NOT NULL COMMENT '操作人',
  version             INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted          TINYINT         DEFAULT 0,
  create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_sku (sku_id),
  INDEX idx_stock_count_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 支付域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_pay_transaction (
  id                  BIGINT UNSIGNED NOT NULL COMMENT '主键',
  payment_no          VARCHAR(32)     NOT NULL COMMENT '支付流水号',
  trade_no            VARCHAR(64)     DEFAULT NULL COMMENT '第三方交易号',
  order_no            VARCHAR(32)     NOT NULL COMMENT '关联订单号',
  user_id             BIGINT UNSIGNED NOT NULL COMMENT '付款用户ID',
  merchant_id         BIGINT UNSIGNED NOT NULL COMMENT '收款商家ID',
  amount              DECIMAL(12,2)   NOT NULL COMMENT '支付金额(元)',
  currency            VARCHAR(8)      NOT NULL DEFAULT 'CNY' COMMENT '币种',
  status              TINYINT         NOT NULL DEFAULT 0 COMMENT '0待支付1已支付2关闭3失败',
  channel             TINYINT         NOT NULL DEFAULT 0 COMMENT '1支付宝2微信',
  channel_response    TEXT            DEFAULT NULL COMMENT '第三方回调原始报文',
  paid_at             DATETIME(3)     DEFAULT NULL COMMENT '支付成功时间',
  expire_at           DATETIME(3)     DEFAULT NULL COMMENT '支付过期时间',
  idempotency_key     VARCHAR(64)     DEFAULT NULL COMMENT '幂等键',
  version             INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '0未删除1已删除',
  create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_transaction_payment_no (payment_no),
  INDEX idx_pay_transaction_order_no (order_no),
  INDEX idx_pay_transaction_user_id (user_id),
  INDEX idx_pay_transaction_merchant_id (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付流水表';

CREATE TABLE IF NOT EXISTS t_pay_refund (
  id                  BIGINT UNSIGNED NOT NULL COMMENT '主键',
  refund_no           VARCHAR(32)     NOT NULL COMMENT '退款流水号',
  payment_no          VARCHAR(32)     NOT NULL COMMENT '关联支付流水号',
  refund_trade_no     VARCHAR(64)     DEFAULT NULL COMMENT '第三方退款交易号',
  amount              DECIMAL(12,2)   NOT NULL COMMENT '退款金额(元)',
  reason              VARCHAR(512)    DEFAULT NULL COMMENT '退款原因',
  status              TINYINT         NOT NULL DEFAULT 0 COMMENT '0待退款1已退款2失败',
  notified_at         DATETIME(3)     DEFAULT NULL COMMENT '退款通知时间',
  idempotency_key     VARCHAR(64)     DEFAULT NULL COMMENT '幂等键',
  version             INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '0未删除1已删除',
  create_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  update_time         DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_pay_refund_refund_no (refund_no),
  INDEX idx_pay_refund_payment_no (payment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款流水表';

-- ============================================================
-- 通知域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_notify_message (
  id              BIGINT          NOT NULL,
  user_id         BIGINT          NOT NULL,
  recipient_type  TINYINT         NOT NULL DEFAULT 1 COMMENT '接收方类型 1=USER 2=MERCHANT 3=PLATFORM',
  message_type    TINYINT         NOT NULL DEFAULT 1 COMMENT '消息类型 1=订单 2=物流 3=退款 4=营销 5=公告 6=系统 7=违规',
  sender_id       BIGINT          DEFAULT NULL COMMENT '发送者ID，系统消息为NULL',
  biz_id          VARCHAR(64)     DEFAULT NULL COMMENT '业务关联ID，如订单号',
  title           VARCHAR(255)    NULL,
  content         VARCHAR(1024)   NULL,
  read_flag       INT             DEFAULT 0,
  version         INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_notify_message_user (user_id),
  INDEX idx_recipient (recipient_type, user_id, read_flag),
  UNIQUE KEY uk_notify_message_receiver_unread (recipient_type, user_id, read_flag, create_time DESC),
  INDEX idx_notify_message_biz (biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_notify_complaint (
  id                  BIGINT          NOT NULL,
  complainant_type    TINYINT         NOT NULL COMMENT '投诉方类型 1=用户 2=商家',
  complainant_id      BIGINT          NOT NULL COMMENT '投诉方ID',
  defendant_type      TINYINT         NOT NULL COMMENT '被诉方类型 1=用户 2=商家',
  defendant_id        BIGINT          NOT NULL COMMENT '被诉方ID',
  order_no            VARCHAR(64)     NULL COMMENT '关联订单号',
  complaint_type      VARCHAR(32)     NOT NULL COMMENT '投诉类型',
  title               VARCHAR(128)    NOT NULL COMMENT '投诉标题',
  content             TEXT            NOT NULL COMMENT '投诉内容',
  status              TINYINT         DEFAULT 0 COMMENT '状态 0=待处理 1=处理中 2=已解决 3=驳回',
  handler_id          BIGINT          NULL COMMENT '处理人ID',
  handler_result      TEXT            NULL COMMENT '处理结果',
  version             INT             NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  is_deleted          TINYINT         DEFAULT 0,
  create_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_complainant (complainant_type, complainant_id),
  INDEX idx_notify_complaint_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_notify_comment (
  id                      BIGINT          NOT NULL,
  order_no                VARCHAR(64)     NOT NULL COMMENT '关联订单号',
  spu_id                  BIGINT          NOT NULL COMMENT '商品SPU ID',
  sku_id                  BIGINT          NULL COMMENT '商品SKU ID，可空',
  user_id                 BIGINT          NOT NULL COMMENT '评价用户ID',
  merchant_id             BIGINT          NOT NULL COMMENT '商家ID，便于按商家查询',
  rating                  TINYINT         NOT NULL COMMENT '评分 1-5',
  content                 TEXT            NOT NULL COMMENT '评价内容',
  images                  TEXT            NULL COMMENT '评价图片，逗号分隔的 objectKey 列表',
  anonymous               TINYINT         DEFAULT 0 COMMENT '是否匿名 0=否 1=是',
  status                  TINYINT         DEFAULT 1 COMMENT '状态 1=正常 2=隐藏 3=已删除',
  merchant_reply          VARCHAR(1024)   NULL COMMENT '商家回复内容',
  merchant_reply_time     DATETIME        NULL COMMENT '商家回复时间',
  is_deleted              TINYINT         DEFAULT 0,
  create_time             DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time             DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_spu (spu_id, status),
  INDEX idx_notify_comment_user (user_id),
  INDEX idx_merchant (merchant_id),
  INDEX idx_notify_comment_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS t_notify_preference (
  id              BIGINT          NOT NULL,
  user_id         BIGINT          NOT NULL COMMENT '用户ID',
  category_code   VARCHAR(32)     NOT NULL COMMENT '通知类别，见 NotifyCategoryEnum',
  site_enabled    TINYINT         DEFAULT 1 COMMENT '站内信开关 0=关 1=开',
  sms_enabled     TINYINT         DEFAULT 1 COMMENT '短信开关 0=关 1=开',
  email_enabled   TINYINT         DEFAULT 1 COMMENT '邮件开关 0=关 1=开',
  is_deleted      TINYINT         DEFAULT 0,
  create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_category (user_id, category_code),
  INDEX idx_notify_preference_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- ID 域
-- ============================================================

CREATE TABLE IF NOT EXISTS t_id_segment (
  id          BIGINT          NOT NULL,
  biz_tag     VARCHAR(64)     NOT NULL,
  current_max BIGINT          DEFAULT 0,
  step        BIGINT          DEFAULT 1000,
  version     INT             DEFAULT 0,
  is_deleted  TINYINT         DEFAULT 0,
  create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_tag (biz_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 种子数据
-- ============================================================

-- 角色：USER / MERCHANT / PLATFORM（V30 原 INSERT 改为 INSERT IGNORE 保证幂等）
INSERT IGNORE INTO t_auth_role (id, role_code, role_name, description) VALUES
  (1, 'USER',     '普通用户',     'C端普通用户'),
  (2, 'MERCHANT', '商家',         'B端商家'),
  (3, 'PLATFORM', '平台管理员',   '平台运营管理员');

-- ID 段：order / pay / stock / user（V10 原 INSERT IGNORE 保留）
INSERT IGNORE INTO t_id_segment (id, biz_tag, current_max, step, version) VALUES
  (1, 'order', 10000, 1000, 0),
  (2, 'pay',   10000, 1000, 0),
  (3, 'stock', 10000, 1000, 0),
  (4, 'user',  10000, 1000, 0);

-- ============================================================
-- 框架表：Seata AT 模式 undo_log（放文件末尾）
-- ============================================================

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id`     BIGINT      NOT NULL COMMENT 'branch transaction id',
  `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
  `context`       VARCHAR(128) NOT NULL COMMENT 'undo log context, such as serialization',
  `rollback_info` LONGBLOB    NOT NULL COMMENT 'rollback info',
  `log_status`    INT         NOT NULL COMMENT '0: normal status, 1: defense status',
  `log_created`   DATETIME(6) NOT NULL COMMENT 'create datetime',
  `log_modified`  DATETIME(6) NOT NULL COMMENT 'modify datetime',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AT transaction mode undo table';

SET FOREIGN_KEY_CHECKS = 1;
