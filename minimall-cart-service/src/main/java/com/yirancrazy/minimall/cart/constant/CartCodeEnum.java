package com.yirancrazy.minimall.cart.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车错误码枚举，定义购物车相关错误码
 * @Version: 1.0
 * @DateTime: 2026/08/02
 */
@Getter
@AllArgsConstructor
public enum CartCodeEnum implements BaseEnum {
    CART_ITEM_NOT_FOUND("14001", "CART_ITEM_NOT_FOUND", "购物车项不存在");

    private final String code;
    private final String alias;
    private final String message;
}
