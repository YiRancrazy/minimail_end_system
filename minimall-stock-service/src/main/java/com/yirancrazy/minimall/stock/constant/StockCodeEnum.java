package com.yirancrazy.minimall.stock.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StockCodeEnum implements BaseEnum {
    STOCK_NOT_FOUND("18001", "STOCK_NOT_FOUND", "SKU 库存不存在"),
    STOCK_INSUFFICIENT("18002", "STOCK_INSUFFICIENT", "库存不足");

    private final String code;
    private final String alias;
    private final String message;
}