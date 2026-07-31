package com.yirancrazy.minimall.goods.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuCodeEnum description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public enum SkuCodeEnum implements BaseEnum {
    SKU_NOT_FOUND("13001", "SKU_NOT_FOUND", "SKU 不存在");

    private final String code;
    private final String alias;
    private final String message;
}