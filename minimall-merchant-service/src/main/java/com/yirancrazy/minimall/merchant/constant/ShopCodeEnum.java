package com.yirancrazy.minimall.merchant.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShopCodeEnum implements BaseEnum {
    SHOP_NOT_FOUND("15001", "SHOP_NOT_FOUND", "店铺不存在");

    private final String code;
    private final String alias;
    private final String message;
}