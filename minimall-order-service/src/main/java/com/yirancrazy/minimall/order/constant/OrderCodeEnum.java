package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
* 订单服务错误码枚举，定义库存锁定失败、订单不存在及支付失败等业务异常信息。
 */
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