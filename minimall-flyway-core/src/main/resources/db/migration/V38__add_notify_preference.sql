-- V38__add_notify_preference.sql
-- 通知偏好表：按 (user_id, category_code) 唯一，记录三个渠道是否开启
CREATE TABLE IF NOT EXISTS t_notify_preference (
  id BIGINT NOT NULL,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  category_code VARCHAR(32) NOT NULL COMMENT '通知类别，见 NotifyCategoryEnum',
  site_enabled TINYINT DEFAULT 1 COMMENT '站内信开关 0=关 1=开',
  sms_enabled TINYINT DEFAULT 1 COMMENT '短信开关 0=关 1=开',
  email_enabled TINYINT DEFAULT 1 COMMENT '邮件开关 0=关 1=开',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_category (user_id, category_code),
  INDEX idx_user (user_id)
);
