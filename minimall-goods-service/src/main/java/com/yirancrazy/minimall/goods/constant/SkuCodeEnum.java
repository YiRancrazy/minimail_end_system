package com.yirancrazy.minimall.goods.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SkuCodeEnum implements BaseEnum {
    SKU_NOT_FOUND("13001", "SKU_NOT_FOUND", "SKU 不存在");

    private final String code;
    private final String alias;
    private final String message;
}