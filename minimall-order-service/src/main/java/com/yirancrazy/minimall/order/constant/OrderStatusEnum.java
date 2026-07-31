package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.base.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单状态枚举，定义订单生命周期各阶段状态及合法转移规则。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Getter
@AllArgsConstructor
public enum OrderStatusEnum implements BaseEnum {
    PENDING(1, "PENDING", "待支付"),
    PAID(2, "PAID", "已支付"),
    SHIPPED(3, "SHIPPED", "已发货"),
    RECEIVED(4, "RECEIVED", "已收货"),
    CANCELLED(5, "CANCELLED", "已取消"),
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

    /**
     * Check if transition from current to target is allowed.
     * @param target the target status to transition to
     * @return true if the transition is valid
     */
    public boolean canTransitTo(OrderStatusEnum target) {
        return switch (this) {
            case PENDING -> target == PAID || target == CANCELLED;
            case PAID -> target == SHIPPED || target == REFUNDING;
            case SHIPPED -> target == RECEIVED || target == REFUNDING;
            case REFUNDING -> target == REFUNDED || target == PAID || target == SHIPPED;
            default -> false;
        };
    }
}
