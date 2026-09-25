package com.yirancrazy.minimall.cart.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItem MyBatis Mapper 接口，映射CartItem表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface CartItemMapper extends BaseMapper<CartItemPO> {

    /**
     * 原子新增或累加购物车数量，已存在时由数据库在数量列上执行增量更新。
     *
     * @param id 新记录候选主键；重复键命中时保留数据库已有主键
     * @param userId 用户ID
     * @param skuId SKU ID
     * @param quantity 本次增加数量
     * @param selected 是否选中：0=否 1=是
     * @return 受影响行数
     */
    @Insert("""
        INSERT INTO t_cart_item
            (id, user_id, sku_id, quantity, selected, version, is_deleted, create_time, update_time)
        VALUES
            (#{id}, #{userId}, #{skuId}, LEAST(#{quantity}, 999), #{selected}, 0, 0,
             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE
            quantity = LEAST(quantity + #{quantity}, 999),
            version = version + 1,
            update_time = CURRENT_TIMESTAMP
        """)
    int upsertQuantity(@Param("id") Long id, @Param("userId") Long userId, @Param("skuId") Long skuId,
                       @Param("quantity") Integer quantity, @Param("selected") Integer selected);
}
