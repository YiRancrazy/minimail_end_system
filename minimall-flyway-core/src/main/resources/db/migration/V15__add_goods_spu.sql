CREATE TABLE IF NOT EXISTS t_goods_spu (
  id BIGINT NOT NULL,
  spu_no VARCHAR(32) NOT NULL,
  merchant_id BIGINT NOT NULL,
  category_id BIGINT NOT NULL,
  title VARCHAR(128) NOT NULL,
  subtitle VARCHAR(255),
  main_image_url VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 0,
  publish_at DATETIME(3) NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_goods_spu_spu_no (spu_no),
  KEY idx_goods_spu_merchant_status (merchant_id, status, update_time)
);
