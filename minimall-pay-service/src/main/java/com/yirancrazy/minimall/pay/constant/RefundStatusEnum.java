package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatusEnum implements BaseEnum {
    PENDING(0, "PENDING", "待发起"),
    SUCCESS(1, "SUCCESS", "退款成功"),
    FAILED(2, "FAILED", "退款失败"),
    CLOSED(3, "CLOSED", "已关闭");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}