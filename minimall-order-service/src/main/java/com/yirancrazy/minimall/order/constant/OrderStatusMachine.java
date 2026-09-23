package com.yirancrazy.minimall.order.constant;

import org.springframework.stereotype.Component;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单状态机，封装订单状态转换规则的校验与推进。无状态单例，由 Spring 容器管理。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Component
public class OrderStatusMachine {

    /**
     * 校验从当前状态到目标状态是否允许转换。
     * @param from 当前订单状态
     * @param target 目标订单状态
     * @return true 如果转换合法
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
     * 推进订单状态，校验失败抛出 BizException。
     * @param order 待推进的订单持久化对象
     * @param target 目标状态
     * @throws BizException 当状态转换非法时
     */
    public void transit(OrderPO order, OrderStatusEnum target) {
        OrderStatusEnum current = fromCode(order.getStatus());
        if (!canTransitTo(current, target)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        order.setStatus(target.intCode());
    }

    /**
     * 由整型状态码解析为订单状态枚举。
     * @param code 持久化的整型状态码
     * @return 对应的订单状态枚举
     * @throws BizException 当状态码非法时
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
