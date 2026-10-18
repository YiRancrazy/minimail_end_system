package com.yirancrazy.minimall.merchant.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


@Getter

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopCodeEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@AllArgsConstructor
public enum ShopCodeEnum implements BaseEnum {
    SHOP_NOT_FOUND("15001", "SHOP_NOT_FOUND", "店铺不存在"),
    SHOP_STATUS_INVALID("15004", "SHOP_STATUS_INVALID", "店铺状态非法"),
    SHOP_MERCHANT_NOT_APPROVED("15005", "SHOP_MERCHANT_NOT_APPROVED", "商家资质未审核通过，暂不可开店"),
    SHOP_STATUS_FORBIDDEN("15006", "SHOP_STATUS_FORBIDDEN", "商家无权设置该店铺状态");

    private final String code;
    private final String alias;
    private final String message;
}