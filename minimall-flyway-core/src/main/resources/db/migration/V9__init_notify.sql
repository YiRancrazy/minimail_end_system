CREATE TABLE IF NOT EXISTS t_notify_message (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(255),
  content VARCHAR(1024),
  read_flag INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user (user_id)
);