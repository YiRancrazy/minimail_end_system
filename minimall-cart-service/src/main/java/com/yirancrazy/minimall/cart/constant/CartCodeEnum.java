package com.yirancrazy.minimall.cart.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartCodeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Getter
@AllArgsConstructor
public enum CartCodeEnum implements BaseEnum {
    CART_ITEM_NOT_FOUND("14001", "CART_ITEM_NOT_FOUND", "购物车项不存在"),
    MOVE_TO_FAVORITE_FAIL("14002", "MOVE_TO_FAVORITE_FAIL", "移入收藏夹失败");

    private final String code;
    private final String alias;
    private final String message;
}
