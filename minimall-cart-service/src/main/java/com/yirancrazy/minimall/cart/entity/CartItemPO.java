package com.yirancrazy.minimall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
* 购物车项持久化实体，对应 t_cart_item 表，记录用户、SKU、数量与选中状态。
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