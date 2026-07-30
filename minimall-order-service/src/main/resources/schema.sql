CREATE TABLE IF NOT EXISTS t_order (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_id BIGINT,
  pay_id BIGINT,
  sku_id BIGINT NOT NULL,
  quantity INT DEFAULT 1,
  amount DECIMAL(12,2) DEFAULT 0,
  status VARCHAR(32) DEFAULT 'PENDING_PAY',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user (user_id),
  INDEX idx_status (status)
);