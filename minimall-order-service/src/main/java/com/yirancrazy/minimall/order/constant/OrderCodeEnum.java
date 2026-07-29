package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderCodeEnum implements BaseEnum {
    STOCK_RESERVE_FAIL("11001", "STOCK_RESERVE_FAIL", "库存锁定失败"),
    ORDER_NOT_FOUND("11002", "ORDER_NOT_FOUND", "订单不存在"),
    PAY_FAIL("11003", "PAY_FAIL", "支付失败");

    private final String code;
    private final String alias;
    private final String message;
}