CREATE TABLE IF NOT EXISTS t_merchant_withdraw (
  id BIGINT NOT NULL,
  merchant_id BIGINT NOT NULL,
  withdraw_no VARCHAR(32) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status TINYINT NOT NULL COMMENT '1=待审核 2=已通过 3=已拒绝 4=已打款',
  reason VARCHAR(255),
  applied_at DATETIME,
  reviewed_at DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_withdraw_no (withdraw_no),
  INDEX idx_merchant_id (merchant_id)
);
