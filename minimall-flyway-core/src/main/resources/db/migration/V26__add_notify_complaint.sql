-- V26__add_notify_complaint.sql
-- 投诉表
CREATE TABLE IF NOT EXISTS t_notify_complaint (
  id BIGINT NOT NULL,
  complainant_type TINYINT NOT NULL COMMENT '投诉方类型 1=用户 2=商家',
  complainant_id BIGINT NOT NULL COMMENT '投诉方ID',
  defendant_type TINYINT NOT NULL COMMENT '被诉方类型 1=用户 2=商家',
  defendant_id BIGINT NOT NULL COMMENT '被诉方ID',
  order_no VARCHAR(64) COMMENT '关联订单号',
  complaint_type VARCHAR(32) NOT NULL COMMENT '投诉类型',
  title VARCHAR(128) NOT NULL COMMENT '投诉标题',
  content TEXT NOT NULL COMMENT '投诉内容',
  status TINYINT DEFAULT 0 COMMENT '状态 0=待处理 1=处理中 2=已解决 3=已驳回',
  handler_id BIGINT COMMENT '处理人ID',
  handler_result TEXT COMMENT '处理结果',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_complainant (complainant_type, complainant_id),
  INDEX idx_status (status)
);
