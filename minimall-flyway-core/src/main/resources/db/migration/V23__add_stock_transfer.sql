-- V23__add_stock_transfer.sql
-- 跨商家库存调拨记录表
CREATE TABLE IF NOT EXISTS t_stock_transfer (
  id BIGINT NOT NULL,
  from_sku_id BIGINT NOT NULL COMMENT '调出SKU',
  to_sku_id BIGINT NOT NULL COMMENT '调入SKU',
  quantity BIGINT NOT NULL COMMENT '调拨数量',
  reason VARCHAR(256) COMMENT '调拨原因',
  operator_id BIGINT NOT NULL COMMENT '操作人（平台管理员）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_from_sku (from_sku_id),
  INDEX idx_to_sku (to_sku_id)
);
