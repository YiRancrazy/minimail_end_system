-- 商品 SKU 按 SPU 批量查询走 IN (spu_id)，补索引消除全表扫描
ALTER TABLE t_sku ADD KEY idx_goods_sku_spu (spu_id);
