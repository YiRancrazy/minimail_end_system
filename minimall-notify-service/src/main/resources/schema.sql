CREATE TABLE IF NOT EXISTS t_notify_message (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  recipient_type INT DEFAULT 1,
  message_type INT DEFAULT 6,
  sender_id BIGINT,
  biz_id VARCHAR(64),
  title VARCHAR(255),
  content VARCHAR(1024),
  read_flag INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user (user_id)
);

CREATE TABLE IF NOT EXISTS t_notify_complaint (
  id BIGINT NOT NULL,
  complainant_type TINYINT NOT NULL,
  complainant_id BIGINT NOT NULL,
  defendant_type TINYINT NOT NULL,
  defendant_id BIGINT NOT NULL,
  order_no VARCHAR(64),
  complaint_type VARCHAR(32) NOT NULL,
  title VARCHAR(128) NOT NULL,
  content TEXT NOT NULL,
  status TINYINT DEFAULT 0,
  handler_id BIGINT,
  handler_result TEXT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_complainant (complainant_type, complainant_id),
  INDEX idx_status (status)
);
