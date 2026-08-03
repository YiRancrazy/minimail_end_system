CREATE TABLE IF NOT EXISTS t_order_logistics (
  id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  node VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  created_time DATETIME,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_order_id (order_id)
);
