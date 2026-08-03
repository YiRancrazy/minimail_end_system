CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT,
  pay_id BIGINT,
  sku_id BIGINT,
  quantity INT,
  amount DECIMAL(12,2) DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  refund_from_status TINYINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user (user_id),
  INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS t_order_item (
  id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  sku_name VARCHAR(256),
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_order (order_id)
);