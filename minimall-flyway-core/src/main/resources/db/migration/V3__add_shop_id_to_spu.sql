-- t_goods_spu 新增归属店铺列：商品必须挂靠营业中店铺，杜绝商家无店铺发布商品
ALTER TABLE t_goods_spu
    ADD COLUMN shop_id BIGINT NULL COMMENT '所属店铺ID' AFTER merchant_id;

CREATE INDEX idx_goods_spu_shop ON t_goods_spu (shop_id);
