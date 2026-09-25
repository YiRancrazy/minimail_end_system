ALTER TABLE t_order
    ADD COLUMN refund_request_no VARCHAR(64) NULL COMMENT '退款请求幂等键' AFTER refund_amount;

ALTER TABLE t_pay_refund
    ADD COLUMN refund_request_no VARCHAR(64) NULL COMMENT '退款请求幂等键' AFTER refund_no;

UPDATE t_pay_refund
SET refund_request_no = refund_no
WHERE refund_request_no IS NULL;

ALTER TABLE t_pay_refund
    MODIFY COLUMN refund_request_no VARCHAR(64) NOT NULL COMMENT '退款请求幂等键';

ALTER TABLE t_pay_refund
    ADD UNIQUE KEY uk_pay_refund_request_no (refund_request_no, is_deleted);
