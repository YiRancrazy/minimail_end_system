package com.yirancrazy.minimall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项持久化实体，对应 t_cart_item 表，记录用户、SKU、数量与选中状态。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart_item")
public class CartItemPO extends BasePO {
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;
}