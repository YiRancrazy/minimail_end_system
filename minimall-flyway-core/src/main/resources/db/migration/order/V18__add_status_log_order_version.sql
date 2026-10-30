ALTER TABLE t_order_status_log
    ADD COLUMN order_version INT DEFAULT NULL COMMENT '订单乐观锁版本（本次转换后），用于状态日志幂等去重' AFTER to_status;

CREATE UNIQUE INDEX uk_order_status_log_order_version
    ON t_order_status_log (order_id, order_version, is_deleted);
