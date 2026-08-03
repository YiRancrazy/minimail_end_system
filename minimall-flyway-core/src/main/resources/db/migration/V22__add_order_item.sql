-- Multi-SKU order support: create order_item table and make order.sku_id/quantity nullable
CREATE TABLE IF NOT EXISTS t_order_item (
  id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  sku_name VARCHAR(256) DEFAULT NULL COMMENT 'SKU快照名称',
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL COMMENT '下单时单价快照',
  amount DECIMAL(12,2) NOT NULL COMMENT '行金额 = unit_price * quantity',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_order (order_id)
);

-- Legacy single-SKU fields become nullable for multi-SKU orders
ALTER TABLE t_order MODIFY COLUMN sku_id BIGINT DEFAULT NULL;
ALTER TABLE t_order MODIFY COLUMN quantity INT DEFAULT NULL;
