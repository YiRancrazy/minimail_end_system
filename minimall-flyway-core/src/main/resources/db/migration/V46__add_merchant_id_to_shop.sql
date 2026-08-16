-- V46__add_merchant_id_to_shop.sql
-- 店铺归属商家ID：为商家端店铺接口（查询/创建/更新/删除）提供越权校验依据
ALTER TABLE t_merch_shop ADD COLUMN merchant_id BIGINT COMMENT '所属商家ID' AFTER id;
CREATE INDEX idx_merch_shop_merchant ON t_merch_shop(merchant_id);

-- 存量回填说明：历史数据无归属，本迁移不做自动回填；
-- 存量 dev 数据由运营脚本按商家与店铺的既有关系分批 UPDATE t_merch_shop.merchant_id；
-- 回填完成前，历史店铺对商家端按"店铺不存在"处理（归属校验失败即视为不可见）。
