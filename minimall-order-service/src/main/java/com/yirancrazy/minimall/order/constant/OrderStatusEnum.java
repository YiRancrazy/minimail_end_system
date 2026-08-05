package com.yirancrazy.minimall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单状态枚举，定义订单生命周期各阶段状态。状态转换规则由 OrderStatusMachine 封装。
 * @Version: 1.1
 * @DateTime: 2026/8/5
 **/
@Getter
@AllArgsConstructor
public enum OrderStatusEnum implements BaseEnum {
    PENDING(1, "PENDING", "待支付"),
    PAID(2, "PAID", "已支付"),
    SHIPPED(3, "SHIPPED", "已发货"),
    COMPLETED(4, "COMPLETED", "已完成"),
    CANCELED(5, "CANCELED", "已取消"),
    REFUNDING(6, "REFUNDING", "退款中"),
    REFUNDED(7, "REFUNDED", "已退款");

    private final int code;
    private final String alias;
    private final String message;

    @Override
    public String getCode() {
        return String.valueOf(code);
    }

    /**
     * Get the integer status code for persistence.
     * @return the integer code value
     */
    public int intCode() {
        return code;
    }
}
