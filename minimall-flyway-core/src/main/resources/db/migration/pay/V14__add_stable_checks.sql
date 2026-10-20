ALTER TABLE t_pay_transaction
    ADD CONSTRAINT chk_pay_transaction_values
        CHECK (status BETWEEN 1 AND 7 AND amount >= 0);

ALTER TABLE t_pay_refund
    ADD CONSTRAINT chk_pay_refund_values
        CHECK (status BETWEEN 0 AND 3 AND amount >= 0 AND retry_count >= 0);
