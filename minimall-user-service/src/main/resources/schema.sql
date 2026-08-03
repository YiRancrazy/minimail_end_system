CREATE TABLE IF NOT EXISTS t_user (
  id BIGINT NOT NULL,
  username VARCHAR(64) NOT NULL,
  nickname VARCHAR(64),
  phone VARCHAR(32),
  email VARCHAR(128),
  avatar VARCHAR(512),
  gender TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
);

CREATE TABLE IF NOT EXISTS t_user_address (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  receiver_name VARCHAR(64) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  province VARCHAR(32) NOT NULL,
  city VARCHAR(32) NOT NULL,
  district VARCHAR(32) NOT NULL,
  detail_address VARCHAR(256) NOT NULL,
  is_default TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user_id (user_id)
);
