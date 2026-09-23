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
    REFUND_CONFLICT("40007", "REFUND_CONFLICT", "Payment state does not allow refund"),
    REFUND_EXCEED("40008", "REFUND_EXCEED", "Total refund amount exceeds payment amount"),
    PAY_AMOUNT_MISMATCH("40009", "PAY_AMOUNT_MISMATCH", "支付金额与订单应付金额不一致"),
    PAY_ORDER_STATUS_INVALID("40010", "PAY_ORDER_STATUS_INVALID", "订单状态不允许创建支付"),
    PAY_CREATE_CONFLICT("40011", "PAY_CREATE_CONFLICT", "支付单创建冲突，请稍后重试"),
    PAY_ORDER_ADVANCE_FAILED("40012", "PAY_ORDER_ADVANCE_FAILED", "订单支付状态推进失败"),
    PAY_AMOUNT_SCALE_INVALID("40013", "PAY_AMOUNT_SCALE_INVALID", "金额最多支持两位小数"),
    PAY_ID_GENERATE_FAIL("40014", "PAY_ID_GENERATE_FAIL", "全局ID生成失败");

    private final String code;
    private final String alias;
    private final String message;
}
