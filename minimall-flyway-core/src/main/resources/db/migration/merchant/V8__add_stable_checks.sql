ALTER TABLE t_merch_merchant
    ADD CONSTRAINT chk_merch_audit_status
        CHECK (audit_status BETWEEN 0 AND 2);

ALTER TABLE t_merch_shop
    ADD CONSTRAINT chk_merch_shop_status
        CHECK (status BETWEEN 1 AND 3);
