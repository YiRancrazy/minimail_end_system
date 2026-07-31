package com.yirancrazy.minimall.stock.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 库存Stock错误码枚举，定义库存相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter
@AllArgsConstructor
public enum StockCodeEnum implements BaseEnum {
    STOCK_NOT_FOUND("18001", "STOCK_NOT_FOUND", "SKU 库存不存在"),
    STOCK_INSUFFICIENT("18002", "STOCK_INSUFFICIENT", "库存不足"),
    THRESHOLD_INVALID("60001", "THRESHOLD_INVALID", "Alert threshold must be >= 0"),
    ADJUST_QUANTITY_ZERO("60002", "ADJUST_QUANTITY_ZERO", "Adjust quantity cannot be zero");

    private final String code;
    private final String alias;
    private final String message;
}