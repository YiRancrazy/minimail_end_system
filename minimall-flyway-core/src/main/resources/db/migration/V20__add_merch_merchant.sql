CREATE TABLE IF NOT EXISTS t_merch_merchant (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  merchant_name VARCHAR(64) NOT NULL,
  license_no VARCHAR(64) NOT NULL,
  audit_status TINYINT NOT NULL DEFAULT 0,
  audit_reason VARCHAR(255),
  audit_at DATETIME(3),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_merch_merchant_user (user_id),
  INDEX idx_merch_status (audit_status)
);
