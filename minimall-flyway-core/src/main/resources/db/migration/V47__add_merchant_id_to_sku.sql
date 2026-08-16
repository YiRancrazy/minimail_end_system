-- V47__add_merchant_id_to_sku.sql
-- 商品 SKU 归属商家ID：为商家端 SKU 接口（查询/创建/更新/删除）提供越权校验依据
ALTER TABLE t_sku ADD COLUMN merchant_id BIGINT COMMENT '所属商家ID' AFTER spu_id;
CREATE INDEX idx_goods_sku_merchant ON t_sku(merchant_id);

-- 存量回填：SKU 归属与其父 SPU 唯一对应（t_goods_spu.merchant_id NOT NULL），
-- 与 V45/V46 不同，此处可从父 SPU 直接派生，故一次性补齐，
-- 避免历史 SKU 因归属为空而绕过商家侧越权校验
UPDATE t_sku SET merchant_id =
  (SELECT spu.merchant_id FROM t_goods_spu spu WHERE spu.id = t_sku.spu_id)
WHERE merchant_id IS NULL;
