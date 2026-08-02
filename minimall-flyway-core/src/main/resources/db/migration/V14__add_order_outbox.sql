CREATE TABLE IF NOT EXISTS t_order_outbox (
  id BIGINT NOT NULL,
  transaction_id VARCHAR(64) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transaction_id (transaction_id)
);
