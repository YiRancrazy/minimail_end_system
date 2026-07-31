package com.yirancrazy.minimall.order.service.impl;

import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderManager orderManager;

    public OrderServiceImpl(OrderManager orderManager) {
        this.orderManager = orderManager;
    }

    @Override
    public Long create(Long userId, Long skuId, Integer quantity) {
        OrderPO po = new OrderPO();
        po.setUserId(userId);
        po.setSkuId(skuId);
        po.setQuantity(quantity);
        po.setStatus(OrderStatusEnum.PENDING.intCode());
        orderManager.save(po);
        log.info("order created, orderId={}, userId={}", po.getId(), userId);
        return po.getId();
    }

    @Override
    public void pay(Long orderId) {
        transitStatus(orderId, OrderStatusEnum.PAID);
        log.info("order paid, orderId={}", orderId);
    }

    @Override
    public void cancel(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.CANCELLED);
        log.info("order cancelled, orderId={}, userId={}", orderId, userId);
    }

    @Override
    public void ship(Long orderId, Long merchantId) {
        transitStatus(orderId, OrderStatusEnum.SHIPPED);
        log.info("order shipped, orderId={}, merchantId={}", orderId, merchantId);
    }

    @Override
    public void confirm(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.RECEIVED);
        log.info("order confirmed, orderId={}, userId={}", orderId, userId);
    }

    @Override
    public void refund(Long orderId) {
        OrderPO po = getOrder(orderId);
        OrderStatusEnum current = fromCode(po.getStatus());
        if (!current.canTransitTo(OrderStatusEnum.REFUNDING)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        po.setRefundFromStatus(po.getStatus());
        po.setStatus(OrderStatusEnum.REFUNDING.intCode());
        orderManager.updateById(po);
        log.info("order refunding, orderId={}", orderId);
    }

    @Override
    public void handleRefundCallback(Long orderId, boolean success) {
        OrderPO po = getOrder(orderId);
        if (success) {
            po.setStatus(OrderStatusEnum.REFUNDED.intCode());
            log.info("order refunded, orderId={}", orderId);
        } else {
            Integer fromStatus = po.getRefundFromStatus();
            po.setStatus(fromStatus != null ? fromStatus : OrderStatusEnum.PAID.intCode());
            log.info("order refund failed, reverted, orderId={}", orderId);
        }
        orderManager.updateById(po);
    }

    @Override
    public Integer getStatus(Long orderId) {
        return getOrder(orderId).getStatus();
    }

    private OrderPO getOrder(Long orderId) {
        OrderPO po = orderManager.getById(orderId);
        if (po == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        return po;
    }

    private void transitStatus(Long orderId, OrderStatusEnum target) {
        OrderPO po = getOrder(orderId);
        OrderStatusEnum current = fromCode(po.getStatus());
        if (!current.canTransitTo(target)) {
            throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
        }
        po.setStatus(target.intCode());
        orderManager.updateById(po);
    }

    private OrderStatusEnum fromCode(Integer code) {
        for (OrderStatusEnum e : OrderStatusEnum.values()) {
            if (e.getCode().equals(String.valueOf(code))) {
                return e;
            }
        }
        throw new BizException(OrderCodeEnum.ORDER_STATUS_TRANSITION_INVALID);
    }
}
