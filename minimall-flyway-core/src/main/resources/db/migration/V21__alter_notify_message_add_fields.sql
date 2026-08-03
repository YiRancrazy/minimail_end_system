-- 扩展通知表：支持多角色接收方、消息类型、发送者与业务关联
ALTER TABLE t_notify_message
  ADD COLUMN recipient_type TINYINT NOT NULL DEFAULT 1 COMMENT '接收方类型 1=USER 2=MERCHANT 3=PLATFORM',
  ADD COLUMN message_type TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型 1=订单 2=物流 3=退款 4=营销 5=公告 6=系统 7=违规',
  ADD COLUMN sender_id BIGINT DEFAULT NULL COMMENT '发送者ID，系统消息为NULL',
  ADD COLUMN biz_id VARCHAR(64) DEFAULT NULL COMMENT '业务关联ID，如订单号',
  ADD INDEX idx_recipient (recipient_type, user_id, read_flag);
