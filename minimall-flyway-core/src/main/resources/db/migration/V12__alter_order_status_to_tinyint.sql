-- V12__alter_order_status_to_tinyint.sql

-- Migrate existing string status to TINYINT
UPDATE t_order SET status = '1' WHERE status = 'PENDING';
UPDATE t_order SET status = '2' WHERE status = 'PAID';
UPDATE t_order SET status = '3' WHERE status = 'SHIPPED';
UPDATE t_order SET status = '4' WHERE status = 'RECEIVED';
UPDATE t_order SET status = '5' WHERE status = 'CANCELLED';

ALTER TABLE t_order MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '1待支付2已支付3已发货4已收货5已取消6退款中7已退款';

ALTER TABLE t_order ADD COLUMN refund_from_status TINYINT COMMENT '退款前状态，用于退款失败回退';
