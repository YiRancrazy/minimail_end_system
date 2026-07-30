package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付服务业务错误码枚举，遵循 17xxx 段，统一 code/alias/message 三元组。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Getter
@AllArgsConstructor
public enum PayCodeEnum implements BaseEnum {
    PAY_NOT_FOUND("17001", "PAY_NOT_FOUND", "支付单不存在"),
    PAY_NOT_SUCCESS("40002", "PAY_NOT_SUCCESS", "Payment not successful"),
    REFUND_AMOUNT_EXCEED("40003", "REFUND_AMOUNT_EXCEED", "Refund amount exceeds payment amount"),
    REFUND_FAILED("40004", "REFUND_FAILED", "Refund failed");

    private final String code;
    private final String alias;
    private final String message;
}