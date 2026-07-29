package com.yirancrazy.minimall.order.service.impl;

import com.yirancrazy.minimall.order.constant.OrderCodeEnum;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.LocalEventBus;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Iter-4: cross-process notify. pay() no longer relies on LocalEventBus for
 * OrderPaid (which is in-process only). It instead synchronously calls
 * NotifyFeignClient.push() which targets the notify-service HTTP push endpoint.
 * The LocalEventBus field is retained for any future in-process subscribers
 * but is intentionally unused here.
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderManager orderManager;
    private final StockFeignClient stockFeign;
    private final PayFeignClient payFeign;
    private final NotifyFeignClient notifyFeign;
    private final LocalEventBus eventBus;

    public OrderServiceImpl(OrderManager orderManager,
                            StockFeignClient stockFeign,
                            PayFeignClient payFeign,
                            NotifyFeignClient notifyFeign,
                            LocalEventBus eventBus) {
        this.orderManager = orderManager;
        this.stockFeign = stockFeign;
        this.payFeign = payFeign;
        this.notifyFeign = notifyFeign;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, Long skuId, Integer quantity) {
        boolean reserved = stockFeign.reserve(new StockReserveDTO(skuId, quantity));
        if (!reserved) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }

        OrderPO order = new OrderPO();
        order.setUserId(userId);
        order.setSkuId(skuId);
        order.setQuantity(quantity);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus("PENDING_PAY");
        orderManager.save(order);

        Long payId = payFeign.create(new PayCreateDTO(order.getId(), order.getAmount()));
        order.setPayId(payId);
        orderManager.updateById(order);

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pay(Long orderId) {
        OrderPO order = orderManager.getById(orderId);
        if (order == null) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        if (!"PENDING_PAY".equals(order.getStatus())) {
            log.warn("order {} status={}, skip pay", orderId, order.getStatus());
            return false;
        }
        Boolean callbackOk = payFeign.callback(order.getPayId());
        if (callbackOk == null || !callbackOk) {
            throw new BizException(OrderCodeEnum.PAY_FAIL);
        }
        order.setStatus("PAID");
        orderManager.updateById(order);

        notifyPaid(order);

        return true;
    }

    @Override
    public String status(Long orderId) {
        OrderPO order = orderManager.getById(orderId);
        return order == null ? "UNKNOWN" : order.getStatus();
    }

    /**
     * Cross-process notify via Feign. Failure is logged but does not roll back
     * the paid state (notify is best-effort; eventual delivery handled by
     * future worker/retry queue).
     */
    private void notifyPaid(OrderPO order) {
        NotifyEventDTO dto = new NotifyEventDTO(
            order.getUserId(),
            "订单支付成功",
            "订单 " + order.getId() + " 已支付，金额 " + order.getAmount());
        Boolean ok = notifyFeign.push(dto);
        if (ok == null || !ok) {
            log.warn("notify push failed for order {}, user={}", order.getId(), order.getUserId());
        } else {
            log.info("notify pushed for order {}", order.getId());
        }
        OrderPaidDTO event = new OrderPaidDTO(order.getId(), order.getUserId(),
            order.getAmount(), LocalDateTime.now().toString());
        eventBus.publish(event);
    }
}