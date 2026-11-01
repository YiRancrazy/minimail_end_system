package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 集中定义支付与退款业务的稳定错误码。
 * 异常码用于 `BizException` 和接口响应，调用方依赖 code 判断支付单、金额及退款冲突语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum PayCodeEnum implements BaseEnum {
    PAY_NOT_FOUND("17001", "PAY_NOT_FOUND", "支付单不存在"),
    PAY_NOT_SUCCESS("40002", "PAY_NOT_SUCCESS", "支付未成功"),
    REFUND_AMOUNT_EXCEED("40003", "REFUND_AMOUNT_EXCEED", "退款金额超过支付金额"),
    REFUND_FAILED("40004", "REFUND_FAILED", "退款失败"),
    PAY_CHANNEL_UNSUPPORTED("40005", "PAY_CHANNEL_UNSUPPORTED", "支付渠道不支持"),
    REFUND_CONFLICT("40007", "REFUND_CONFLICT", "支付状态不允许退款"),
    REFUND_EXCEED("40008", "REFUND_EXCEED", "累计退款金额超过支付金额"),
    PAY_AMOUNT_MISMATCH("40009", "PAY_AMOUNT_MISMATCH", "支付金额与订单应付金额不一致"),
    PAY_ORDER_STATUS_INVALID("40010", "PAY_ORDER_STATUS_INVALID", "订单状态不允许创建支付"),
    PAY_CREATE_CONFLICT("40011", "PAY_CREATE_CONFLICT", "支付单创建冲突，请稍后重试"),
    PAY_ORDER_ADVANCE_FAILED("40012", "PAY_ORDER_ADVANCE_FAILED", "订单支付状态推进失败"),
    PAY_AMOUNT_SCALE_INVALID("40013", "PAY_AMOUNT_SCALE_INVALID", "金额最多支持两位小数"),
    PAY_ID_GENERATE_FAIL("40014", "PAY_ID_GENERATE_FAIL", "全局ID生成失败"),
    PAY_CLOSE_STATUS_INVALID("40015", "PAY_CLOSE_STATUS_INVALID", "支付状态不允许关闭"),
    PAY_CLOSE_FAILED("40016", "PAY_CLOSE_FAILED", "关闭支付失败"),
    PAY_ALIPAY_STATUS_NULL("40017", "PAY_ALIPAY_STATUS_NULL", "支付宝支付状态为空"),
    PAY_NOT_PENDING("40018", "PAY_NOT_PENDING", "支付单不处于待支付状态");

    private final String code;
    private final String alias;
    private final String message;
}
