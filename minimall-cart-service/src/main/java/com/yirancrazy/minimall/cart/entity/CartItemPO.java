package com.yirancrazy.minimall.cart.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_cart_item")
public class CartItemPO extends BasePO {
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Integer selected;
}