package com.yirancrazy.minimall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItem持久化对象，映射cartitem表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class CartItemPO extends BasePO {
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;
}