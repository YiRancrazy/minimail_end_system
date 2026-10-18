package com.yirancrazy.minimall.pay.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayChannelEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }


    /**
     * 执行 fromValue 方法。
     * @param value value 参数
     * @return 处理结果
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
        throw new IllegalArgumentException("未知支付渠道：" + value);
    }
}
