package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品Sku错误码枚举，定义商品相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter

@AllArgsConstructor
public enum SkuCodeEnum implements BaseEnum {
    SKU_NOT_FOUND("13001", "SKU_NOT_FOUND", "SKU 不存在");

    private final String code;
    private final String alias;
    private final String message;
}