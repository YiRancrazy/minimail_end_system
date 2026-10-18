ALTER TABLE t_pay_transaction
    ADD COLUMN active_order_no VARCHAR(32) NULL COMMENT '活跃支付订单唯一键：待支付/成功/退款中/已退款/冻结时等于 order_no，失败或关闭时为 NULL' AFTER order_no;

UPDATE t_pay_transaction
SET active_order_no = order_no
WHERE is_deleted = 0 AND status NOT IN (3, 4);

ALTER TABLE t_pay_transaction
    ADD UNIQUE KEY uk_pay_transaction_active_order (active_order_no, is_deleted);
