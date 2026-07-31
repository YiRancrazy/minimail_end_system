package com.yirancrazy.minimall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单Order错误码枚举，定义订单相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public enum OrderCodeEnum implements BaseEnum {
    STOCK_RESERVE_FAIL("11001", "STOCK_RESERVE_FAIL", "库存锁定失败"),
    ORDER_NOT_FOUND("11002", "ORDER_NOT_FOUND", "订单不存在"),
    PAY_FAIL("11003", "PAY_FAIL", "支付失败"),
    ORDER_STATUS_TRANSITION_INVALID("50001", "ORDER_STATUS_TRANSITION_INVALID", "订单状态流转不合法"),
    ORDER_ALREADY_CANCELLED("50002", "ORDER_ALREADY_CANCELLED", "订单已取消");

    private final String code;
    private final String alias;
    private final String message;
}