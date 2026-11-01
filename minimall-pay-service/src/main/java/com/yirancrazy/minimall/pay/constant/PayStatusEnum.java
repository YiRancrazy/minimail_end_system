package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义支付流水从待支付到退款、冻结等状态。
 * 持久化使用整数 code；回调、退款和补偿任务均以该状态机判断是否允许推进。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Getter
@AllArgsConstructor
public enum PayStatusEnum implements BaseEnum {
    PENDING(1, "PENDING", "待支付"),
    SUCCESS(2, "SUCCESS", "支付成功"),
    FAILED(3, "FAILED", "支付失败"),
    CLOSED(4, "CLOSED", "已关闭"),
    REFUNDING(5, "REFUNDING", "退款中"),
    REFUNDED(6, "REFUNDED", "已退款"),
    FROZEN(7, "FROZEN", "已冻结");

    private final int code;
    private final String alias;
    private final String message;

    /**
     * 返回支付状态的十进制整数字符串，以兼容 `BaseEnum`。
     *
     * @return 支付状态 code 的字符串形式
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
