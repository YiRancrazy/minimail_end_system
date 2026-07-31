package com.yirancrazy.minimall.pay.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayCodeEnum description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public enum PayCodeEnum implements BaseEnum {
    PAY_NOT_FOUND("17001", "PAY_NOT_FOUND", "支付单不存在"),
    PAY_NOT_SUCCESS("40002", "PAY_NOT_SUCCESS", "Payment not successful"),
    REFUND_AMOUNT_EXCEED("40003", "REFUND_AMOUNT_EXCEED", "Refund amount exceeds payment amount"),
    REFUND_FAILED("40004", "REFUND_FAILED", "Refund failed");

    private final String code;
    private final String alias;
    private final String message;
}