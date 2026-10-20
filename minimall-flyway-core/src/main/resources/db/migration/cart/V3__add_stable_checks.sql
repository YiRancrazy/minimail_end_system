UPDATE t_cart_item
SET quantity = 1
WHERE quantity IS NULL;

ALTER TABLE t_cart_item
    MODIFY COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    ADD CONSTRAINT chk_cart_quantity
        CHECK (quantity > 0);
