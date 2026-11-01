package com.yirancrazy.minimall.pay.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义退款流水从待发起到成功、失败或关闭的状态。
 * 退款记录以 code 持久化，扫描任务据此识别待补偿和最终结果。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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

    /**
     * 返回退款状态的十进制整数字符串，以兼容 `BaseEnum`。
     *
     * @return 退款状态 code 的字符串形式
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }
}
