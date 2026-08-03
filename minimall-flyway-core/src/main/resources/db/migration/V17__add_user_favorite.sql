CREATE TABLE IF NOT EXISTS t_user_favorite (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  sku_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_sku (user_id, sku_id),
  INDEX idx_user_id (user_id)
);
