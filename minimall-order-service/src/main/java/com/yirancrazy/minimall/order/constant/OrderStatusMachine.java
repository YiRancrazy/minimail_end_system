package com.yirancrazy.minimall.order.constant;

import org.springframework.stereotype.Component;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderStatusMachine 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Component
public class OrderStatusMachine {


    /**
     * 执行 canTransitTo 方法。
     * @param from from 参数
     * @param target target 参数
     * @return 是否处理成功
     */
    public boolean canTransitTo(OrderStatusEnum from, OrderStatusEnum target) {
        if (from == null || target == null) {
            return false;
        }
        return switch (from) {
            case PENDING -> target == OrderStatusEnum.PAID || target == OrderStatusEnum.CANCELED;
            case PAID -> target == OrderStatusEnum.SHIPPED || target == OrderStatusEnum.REFUNDING;
            case SHIPPED -> target == OrderStatusEnum.COMPLETED || target == OrderStatusEnum.REFUNDING;
            case REFUNDING -> target == OrderStatusEnum.REFUNDED
                || target == OrderStatusEnum.PAID
                || target == OrderStatusEnum.SHIPPED;
            default -> false;
        };
    }


    /**
     * 执行 transit 方法。
     * @param order order 参数
     * @param target target 参数
     */
    public void transit(OrderPO order, OrderStatusEnum target) {
        OrderStatusEnum current = fromCode(order.getStatus());
        if (!canTransitTo(current, target)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        order.setStatus(target.intCode());
    }


    /**
     * 执行 fromCode 方法。
     * @param code code 参数
     * @return 处理结果
     */
    public OrderStatusEnum fromCode(Integer code) {
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            if (e.getCode().equals(String.valueOf(code))) {
                return e;
            }
        }
        throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
    }
}
