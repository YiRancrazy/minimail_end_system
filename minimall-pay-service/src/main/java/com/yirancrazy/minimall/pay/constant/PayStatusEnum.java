package com.yirancrazy.minimall.pay.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

@Getter
@AllArgsConstructor
public enum PayStatusEnum implements BaseEnum {
    PENDING(1, "PENDING", "待支付"),
    SUCCESS(2, "SUCCESS", "支付成功"),
    FAILED(3, "FAILED", "支付失败"),
    CLOSED(4, "CLOSED", "已关闭"),
    REFUNDING(5, "REFUNDING", "退款中"),
    REFUNDED(6, "REFUNDED", "已退款");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}