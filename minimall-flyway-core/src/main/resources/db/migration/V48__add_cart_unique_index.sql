-- V48__add_cart_unique_index.sql
-- 购物车同用户同 SKU 唯一：CartServiceImpl.add 依赖该索引实现重复加入合并数量，防止一人多行
ALTER TABLE t_cart_item ADD UNIQUE KEY uk_user_sku (user_id, sku_id);
