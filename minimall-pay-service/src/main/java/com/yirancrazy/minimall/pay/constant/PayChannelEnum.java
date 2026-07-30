package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PayChannelEnum implements BaseEnum {
    ALIPAY(1, "ALIPAY", "支付宝"),
    WECHAT(2, "WECHAT", "微信支付"),
    BALANCE(3, "BALANCE", "余额");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}