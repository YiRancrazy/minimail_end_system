-- 历史数据保留原值，仅将 transaction_id 重命名为 event_id。
-- 发布器查询待发送消息时必须同时过滤 payload IS NOT NULL。
ALTER TABLE t_order_outbox
    CHANGE COLUMN transaction_id event_id VARCHAR(128) NOT NULL
        COMMENT '唯一业务事件键，例如 ORDER_PAID:<orderId>',
    ADD COLUMN event_type VARCHAR(64) NULL
        COMMENT '事件类型，例如 OrderPaidDTO' AFTER event_id,
    ADD COLUMN aggregate_type VARCHAR(32) NULL
        COMMENT '聚合类型，例如 ORDER' AFTER event_type,
    ADD COLUMN aggregate_id BIGINT NULL
        COMMENT '聚合标识，例如 orderId' AFTER aggregate_type,
    ADD COLUMN topic VARCHAR(128) NULL
        COMMENT 'RocketMQ 主题' AFTER aggregate_id,
    ADD COLUMN tag VARCHAR(128) NULL
        COMMENT 'RocketMQ 标签' AFTER topic,
    ADD COLUMN payload LONGTEXT NULL
        COMMENT '事件 JSON 内容' AFTER tag,
    ADD COLUMN send_status TINYINT NOT NULL DEFAULT 0
        COMMENT '发送状态：0=待发送，1=已发送，2=死信' AFTER payload,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0
        COMMENT '发送重试次数' AFTER send_status,
    ADD COLUMN next_retry_time DATETIME(3) NULL
        COMMENT '下次发送时间' AFTER retry_count,
    ADD COLUMN last_error VARCHAR(512) NULL
        COMMENT '最近一次发送失败原因' AFTER next_retry_time,
    ADD COLUMN sent_time DATETIME(3) NULL
        COMMENT '发送成功时间' AFTER last_error,
    DROP INDEX uk_transaction_id,
    ADD UNIQUE KEY uk_order_outbox_event_id (event_id, is_deleted),
    ADD INDEX idx_order_outbox_dispatch (send_status, next_retry_time, id);
