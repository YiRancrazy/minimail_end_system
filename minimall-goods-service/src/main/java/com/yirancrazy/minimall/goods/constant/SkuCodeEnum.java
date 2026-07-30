package com.yirancrazy.minimall.goods.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
* 商品服务错误码枚举，统一定义 SKU 域业务异常码（如 SKU_NOT_FOUND）及国际化文案。
 */
@Getter
@AllArgsConstructor
public enum SkuCodeEnum implements BaseEnum {
    SKU_NOT_FOUND("13001", "SKU_NOT_FOUND", "SKU 不存在");

    private final String code;
    private final String alias;
    private final String message;
}