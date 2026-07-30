package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
* 商家服务错误码枚举，集中维护店铺不存在等业务异常码及其消息描述。
 */
@Getter
@AllArgsConstructor
public enum ShopCodeEnum implements BaseEnum {
    SHOP_NOT_FOUND("15001", "SHOP_NOT_FOUND", "店铺不存在");

    private final String code;
    private final String alias;
    private final String message;
}