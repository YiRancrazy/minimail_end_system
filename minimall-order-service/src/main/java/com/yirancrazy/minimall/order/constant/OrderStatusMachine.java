package com.yirancrazy.minimall.order.constant;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.entity.OrderPo;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 集中实现订单主状态迁移规则和非法迁移校验。
 *   业务边界：只修改内存中的 OrderPo 状态，不负责数据库更新、状态日志或补偿动作；持久化由调用方事务完成。
 *   使用场景：OrderServiceImpl 在支付、发货、确认、取消和退款链路中调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Component
public class OrderStatusMachine {

    /**
     * 判断订单当前状态能否按状态机进入目标状态。
     *
     * <p>允许待支付到已支付或已取消、已支付到已发货或退款中、已发货到已完成或退款中，退款中可回退到原购买态或转为已退款。
     *
     * @param from 迁移前订单状态
     * @param target 目标订单状态
     * @return 允许该迁移时返回 true；空值或非法迁移返回 false。
     */
    public boolean canTransitTo(OrderStatusEnum from, OrderStatusEnum target) {
        if (from == null || target == null) {
            return false;
        }
        return switch (from) {
            case PENDING -> target == OrderStatusEnum.PAID || target == OrderStatusEnum.CANCELED;
            case PAID -> target == OrderStatusEnum.SHIPPED || target == OrderStatusEnum.REFUNDING;
            case SHIPPED -> target == OrderStatusEnum.COMPLETED || target == OrderStatusEnum.REFUNDING;
            case REFUNDING ->
                    target == OrderStatusEnum.REFUNDED
                            || target == OrderStatusEnum.PAID
                            || target == OrderStatusEnum.SHIPPED;
            default -> false;
        };
    }

    /**
     * 校验并应用一次订单状态迁移。
     *
     * <p>只修改 OrderPo 的内存状态字段；调用方需自行负责事务更新和状态日志，非法迁移会抛出业务异常。
     *
     * @param order 订单持久化对象
     * @param target 目标订单状态
     * @throws BizException 当前订单为空、状态码非法或目标状态不可达。
     */
    public void transit(OrderPo order, OrderStatusEnum target) {
        OrderStatusEnum current = fromCode(order.getStatus());
        if (!canTransitTo(current, target)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        order.setStatus(target.intCode());
    }

    /**
     * 把整数订单状态码解析为枚举。
     *
     * <p>逐项匹配 OrderStatusEnum 的稳定 code；无法匹配时按非法状态迁移处理。
     *
     * @param code 订单状态整数编码
     * @return 匹配到的订单状态枚举。
     * @throws BizException 状态码为空或不在已定义范围内。
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
