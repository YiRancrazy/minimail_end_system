-- 订单模型对齐数据库设计文档 §10.2/§10.3：t_order/t_order_item 补充关键列并回填存量

ALTER TABLE t_order ADD COLUMN order_no VARCHAR(32) NULL COMMENT '业务单号' AFTER id;
ALTER TABLE t_order ADD COLUMN order_type TINYINT NOT NULL DEFAULT 1 COMMENT '1-普通 2-拼团 3-秒杀（预留）' AFTER order_no;
ALTER TABLE t_order ADD COLUMN total_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '应付总额' AFTER amount;
ALTER TABLE t_order ADD COLUMN pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实付金额' AFTER total_amount;
ALTER TABLE t_order ADD COLUMN freight_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '运费' AFTER pay_amount;
ALTER TABLE t_order ADD COLUMN discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '优惠金额' AFTER freight_amount;
ALTER TABLE t_order ADD COLUMN receiver_snapshot_json TEXT NULL COMMENT '下单收货信息快照' AFTER discount_amount;
ALTER TABLE t_order ADD COLUMN pay_expire_at DATETIME NULL COMMENT '支付超时时间' AFTER receiver_snapshot_json;
ALTER TABLE t_order ADD COLUMN paid_at DATETIME NULL COMMENT '支付完成时间' AFTER pay_expire_at;
ALTER TABLE t_order ADD COLUMN received_at DATETIME NULL COMMENT '收货时间' AFTER paid_at;
ALTER TABLE t_order ADD COLUMN closed_at DATETIME NULL COMMENT '关闭时间' AFTER received_at;
ALTER TABLE t_order ADD COLUMN close_reason VARCHAR(128) NULL COMMENT '关闭原因' AFTER closed_at;
ALTER TABLE t_order ADD COLUMN client_ip VARCHAR(45) NULL COMMENT '客户端IP' AFTER close_reason;
ALTER TABLE t_order ADD COLUMN idempotency_key CHAR(36) NULL COMMENT '幂等键' AFTER client_ip;

ALTER TABLE t_order_item ADD COLUMN spu_id BIGINT NULL COMMENT 'SPU ID' AFTER order_id;
ALTER TABLE t_order_item ADD COLUMN merchant_id BIGINT NULL COMMENT '商家ID' AFTER spu_id;
ALTER TABLE t_order_item ADD COLUMN spu_snapshot_json TEXT NULL COMMENT '下单SPU快照' AFTER merchant_id;
ALTER TABLE t_order_item ADD COLUMN sku_snapshot_json TEXT NULL COMMENT '下单SKU快照' AFTER spu_snapshot_json;
ALTER TABLE t_order_item ADD COLUMN sku_image_url VARCHAR(255) NULL COMMENT 'SKU图片' AFTER sku_snapshot_json;
ALTER TABLE t_order_item ADD COLUMN subtotal_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '行小计' AFTER amount;
ALTER TABLE t_order_item ADD COLUMN discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '行优惠' AFTER subtotal_amount;
ALTER TABLE t_order_item ADD COLUMN pay_amount DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '行实付' AFTER discount_amount;
ALTER TABLE t_order_item ADD COLUMN refund_status TINYINT NOT NULL DEFAULT 0 COMMENT '0-无 1-部分退款 2-全额退款' AFTER pay_amount;

-- 存量回填：order_no 用 OD+主键 保证唯一；金额列回填现有 amount
UPDATE t_order SET order_no = CONCAT('OD', id) WHERE order_no IS NULL;
UPDATE t_order SET total_amount = amount, pay_amount = amount WHERE total_amount = 0;
UPDATE t_order SET pay_expire_at = DATE_ADD(create_time, INTERVAL 30 MINUTE) WHERE status = 1 AND pay_expire_at IS NULL;
UPDATE t_order_item SET subtotal_amount = amount, pay_amount = amount WHERE subtotal_amount = 0;

CREATE UNIQUE INDEX uk_order_no ON t_order (order_no);
CREATE INDEX idx_order_pay_expire ON t_order (status, pay_expire_at);
CREATE UNIQUE INDEX uk_order_idem ON t_order (idempotency_key);
CREATE INDEX idx_order_item_merchant ON t_order_item (merchant_id);
