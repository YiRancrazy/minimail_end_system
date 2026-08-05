-- V36: Add order_group_no to t_order for cross-merchant order splitting.
-- One user checkout may split into multiple t_order rows sharing the same order_group_no.
ALTER TABLE t_order ADD COLUMN order_group_no VARCHAR(32) NULL COMMENT '订单组号，跨商家拆单时同一结算单的子订单共享';
CREATE INDEX idx_order_group_no ON t_order(order_group_no);
