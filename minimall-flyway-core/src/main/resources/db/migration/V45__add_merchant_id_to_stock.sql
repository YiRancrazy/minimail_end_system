-- V45__add_merchant_id_to_stock.sql
-- 库存归属商家ID：为商家端库存接口（查询/阈值/调整/流水）提供越权校验依据
ALTER TABLE t_stock ADD COLUMN merchant_id BIGINT COMMENT '所属商家ID' AFTER sku_id;
CREATE INDEX idx_stock_merchant ON t_stock(merchant_id);

-- 存量回填说明：SKU→merchant 映射由 goods-service 提供，本迁移不做自动回填；
-- 存量 dev 数据由运营脚本按 goods 表归属分批 UPDATE t_stock.merchant_id。
