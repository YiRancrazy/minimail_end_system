ALTER TABLE t_order
    ADD COLUMN refund_request_no VARCHAR(64) NULL COMMENT '退款请求幂等键' AFTER refund_amount;
