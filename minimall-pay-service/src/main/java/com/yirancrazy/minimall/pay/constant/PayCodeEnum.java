package com.yirancrazy.minimall.pay.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 支付Pay错误码枚举，定义支付相关错误码
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Getter

@AllArgsConstructor
public enum PayCodeEnum implements BaseEnum {
    PAY_NOT_FOUND("17001", "PAY_NOT_FOUND", "支付单不存在"),
    PAY_NOT_SUCCESS("40002", "PAY_NOT_SUCCESS", "Payment not successful"),
    REFUND_AMOUNT_EXCEED("40003", "REFUND_AMOUNT_EXCEED", "Refund amount exceeds payment amount"),
    REFUND_FAILED("40004", "REFUND_FAILED", "Refund failed"),
    PAY_CHANNEL_UNSUPPORTED("40005", "PAY_CHANNEL_UNSUPPORTED", "Payment channel not supported"),
    WITHDRAW_NOT_FOUND("40006", "WITHDRAW_NOT_FOUND", "提现单不存在"),
    REFUND_CONFLICT("40007", "REFUND_CONFLICT", "Payment state does not allow refund"),
    REFUND_EXCEED("40008", "REFUND_EXCEED", "Total refund amount exceeds payment amount");

    private final String code;
    private final String alias;
    private final String message;
}