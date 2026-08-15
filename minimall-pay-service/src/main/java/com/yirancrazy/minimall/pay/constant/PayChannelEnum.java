package com.yirancrazy.minimall.pay.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 枚举类，定义业务常量。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
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

    /**
     * 反序列化入口，兼容枚举名（ALIPAY，大小写不敏感）与数字 code（1）。
     * 客户端契约不统一（字符串/数字均可能），未知值抛出异常交由全局异常处理返回参数错误。
     * @param value JSON 中 channel 字段值
     * @return 匹配的渠道枚举
     */
    @JsonCreator
    public static PayChannelEnum fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (PayChannelEnum e : values()) {
            if (e.name().equalsIgnoreCase(value) || String.valueOf(e.code).equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown channel: " + value);
    }
}