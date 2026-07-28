CREATE TABLE IF NOT EXISTS t_id_segment (
  id BIGINT NOT NULL,
  biz_tag VARCHAR(64) NOT NULL,
  current_max BIGINT DEFAULT 0,
  step BIGINT DEFAULT 1000,
  version INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_tag (biz_tag)
);

INSERT IGNORE INTO t_id_segment (id, biz_tag, current_max, step, version)
VALUES
  (1, 'order',  10000, 1000, 0),
  (2, 'pay',    10000, 1000, 0),
  (3, 'stock',  10000, 1000, 0),
  (4, 'user',   10000, 1000, 0);