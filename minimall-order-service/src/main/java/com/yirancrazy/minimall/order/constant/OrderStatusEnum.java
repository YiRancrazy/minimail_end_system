package com.yirancrazy.minimall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.yirancrazy.minimall.common.base.BaseEnum;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusEnum 枚举，定义相关业务状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 获取相关数据。
     * @return 处理结果
     */
    @Override
    public String getCode() {
        return String.valueOf(code);
    }


    /**
     * 执行 intCode 方法。
     * @return 业务数值
     */
    public int intCode() {
        return code;
    }
}
