UPDATE t_sku
SET price = 0
WHERE price IS NULL;

ALTER TABLE t_goods_spu
    ADD CONSTRAINT chk_goods_spu_status
        CHECK (status BETWEEN 0 AND 4);

ALTER TABLE t_sku
    MODIFY COLUMN price DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '单价（元）',
    ADD CONSTRAINT chk_sku_values
        CHECK (price >= 0 AND stock >= 0);

ALTER TABLE t_goods_audit_record
    ADD CONSTRAINT chk_goods_audit_decision
        CHECK (decision IN (1, 2));
