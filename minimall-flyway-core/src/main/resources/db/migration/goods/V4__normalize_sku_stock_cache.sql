UPDATE t_sku
SET stock = 0
WHERE stock IS NULL;

ALTER TABLE t_sku
    MODIFY COLUMN stock INT NOT NULL DEFAULT 0
        COMMENT '库存展示缓存，权威库存见t_stock.available';
