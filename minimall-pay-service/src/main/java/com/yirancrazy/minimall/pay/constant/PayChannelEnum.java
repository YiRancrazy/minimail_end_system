package com.yirancrazy.minimall.pay.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义支付渠道编码、别名和展示文案。
 * 服务端当前只受理支付宝渠道；该枚举同时用于请求反序列化和对已存渠道值进行解释。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
     * 返回枚举的十进制整数字符串，以满足 `BaseEnum` 的通用接口。
     *
     * @return 渠道整数编码的字符串形式
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * 将 JSON 渠道值解析为支付渠道枚举。
     * 支持忽略大小写匹配枚举名及十进制 code；空值返回 null，未知值抛出异常。
     *
     * @param value 枚举名或十进制渠道 code
     * @return 匹配的支付渠道，输入为 null 时返回 null
     * @throws IllegalArgumentException 输入值不对应任何支付渠道
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
