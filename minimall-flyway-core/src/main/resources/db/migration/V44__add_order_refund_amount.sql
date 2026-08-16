-- V44__add_order_refund_amount.sql
-- 订单实际退款金额：商家部分退款时记录，供支付侧按该金额退款
ALTER TABLE t_order ADD COLUMN refund_amount DECIMAL(12,2) COMMENT '实际退款金额（元），部分退款时记录' AFTER amount;
