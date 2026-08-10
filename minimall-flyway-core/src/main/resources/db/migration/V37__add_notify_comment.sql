-- V37__add_notify_comment.sql
-- 商品评价表（含商家回复字段）
CREATE TABLE IF NOT EXISTS t_notify_comment (
  id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL COMMENT '关联订单号',
  spu_id BIGINT NOT NULL COMMENT '商品SPU ID',
  sku_id BIGINT COMMENT '商品SKU ID，可空',
  user_id BIGINT NOT NULL COMMENT '评价用户ID',
  merchant_id BIGINT NOT NULL COMMENT '商家ID，便于按商家查询',
  rating TINYINT NOT NULL COMMENT '评分 1-5',
  content TEXT NOT NULL COMMENT '评价内容',
  images TEXT COMMENT '评价图片，逗号分隔的 objectKey 列表',
  anonymous TINYINT DEFAULT 0 COMMENT '是否匿名 0=否 1=是',
  status TINYINT DEFAULT 1 COMMENT '状态 1=正常 2=隐藏 3=已删除',
  merchant_reply VARCHAR(1024) COMMENT '商家回复内容',
  merchant_reply_time DATETIME COMMENT '商家回复时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_spu (spu_id, status),
  INDEX idx_user (user_id),
  INDEX idx_merchant (merchant_id),
  INDEX idx_order (order_no)
);
