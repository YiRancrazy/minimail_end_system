-- V25__add_user_address.sql
-- 收货地址表 + 用户表扩展 avatar/gender 字段
CREATE TABLE IF NOT EXISTS t_user_address (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  receiver_name VARCHAR(64) NOT NULL COMMENT '收货人姓名',
  receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人手机号',
  province VARCHAR(32) NOT NULL COMMENT '省份',
  city VARCHAR(32) NOT NULL COMMENT '城市',
  district VARCHAR(32) NOT NULL COMMENT '区/县',
  detail_address VARCHAR(256) NOT NULL COMMENT '详细地址',
  is_default TINYINT DEFAULT 0 COMMENT '是否默认 0=否 1=是',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_user_id (user_id)
);

-- 用户表扩展头像与性别字段
ALTER TABLE t_user ADD COLUMN avatar VARCHAR(512) COMMENT '头像URL';
ALTER TABLE t_user ADD COLUMN gender TINYINT DEFAULT 0 COMMENT '性别 0=未知 1=男 2=女';
