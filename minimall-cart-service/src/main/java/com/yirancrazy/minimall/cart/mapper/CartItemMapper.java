package com.yirancrazy.minimall.cart.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.cart.entity.CartItemPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CartItemMapper extends BaseMapper<CartItemPO> {


    /**
     * 执行 upsertQuantity 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @param skuId skuId 参数
     * @param quantity quantity 参数
     * @param selected selected 参数
     * @return 业务数值
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
