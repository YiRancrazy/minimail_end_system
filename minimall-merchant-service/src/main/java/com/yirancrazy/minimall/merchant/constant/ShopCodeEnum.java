package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商户Shop错误码枚举，定义商户相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter

@AllArgsConstructor
public enum ShopCodeEnum implements BaseEnum {
    SHOP_NOT_FOUND("15001", "SHOP_NOT_FOUND", "店铺不存在");

    private final String code;
    private final String alias;
    private final String message;
}