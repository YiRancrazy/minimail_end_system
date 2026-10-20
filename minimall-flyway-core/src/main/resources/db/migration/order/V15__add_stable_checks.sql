ALTER TABLE t_order
    ADD CONSTRAINT chk_order_trade_values CHECK (
        order_type BETWEEN 1 AND 3
        AND status BETWEEN 1 AND 7
        AND stock_release_status BETWEEN 0 AND 2
        AND (refund_from_status IS NULL OR refund_from_status BETWEEN 1 AND 7)
        AND (quantity IS NULL OR quantity > 0)
        AND (amount IS NULL OR amount >= 0)
        AND (refund_amount IS NULL OR refund_amount >= 0)
        AND total_refunded_amount >= 0
        AND total_amount >= 0
        AND pay_amount >= 0
        AND freight_amount >= 0
        AND discount_amount >= 0
    );

ALTER TABLE t_order_item
    ADD CONSTRAINT chk_order_item_values CHECK (
        quantity > 0
        AND unit_price >= 0
        AND amount >= 0
        AND subtotal_amount >= 0
        AND discount_amount >= 0
        AND pay_amount >= 0
        AND refund_status IN (0, 1, 2)
    );

ALTER TABLE t_order_status_log
    ADD CONSTRAINT chk_order_status_log_values CHECK (
        (from_status IS NULL OR from_status BETWEEN 1 AND 7)
        AND to_status BETWEEN 1 AND 7
    );

ALTER TABLE t_order_payment_snapshot
    ADD CONSTRAINT chk_order_payment_amount
        CHECK (pay_amount >= 0);
