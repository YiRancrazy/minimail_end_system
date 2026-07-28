package com.yirancrazy.minimall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
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

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderManager orderManager;
    private final StockFeignClient stockFeign;
    private final PayFeignClient payFeign;
    private final LocalEventBus eventBus;

    public OrderServiceImpl(OrderManager orderManager,
                            StockFeignClient stockFeign,
                            PayFeignClient payFeign,
                            LocalEventBus eventBus) {
        this.orderManager = orderManager;
        this.stockFeign = stockFeign;
        this.payFeign = payFeign;
        this.eventBus = eventBus;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, Long skuId, Integer quantity) {
        // 1. 锁库存（Feign, 事务外做；以 fallback 默认 false）
        boolean reserved = stockFeign.reserve(new StockReserveDTO(skuId, quantity));
        if (!reserved) {
            throw new BizException("11001", "STOCK_RESERVE_FAIL", "库存锁定失败");
        }

        // 2. 落单（默认金额 placeholder，Iter-3 接入价格计算）
        OrderPO order = new OrderPO();
        order.setUserId(userId);
        order.setSkuId(skuId);
        order.setQuantity(quantity);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus("PENDING_PAY");
        orderManager.save(order);

        // 3. 创建支付单
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
            throw new BizException("11002", "ORDER_NOT_FOUND", "订单不存在");
        }
        if (!"PENDING_PAY".equals(order.getStatus())) {
            log.warn("order {} status={}, skip pay", orderId, order.getStatus());
            return false;
        }
        // 触发伪支付宝回调（同步推进支付单为 PAID）
        Boolean callbackOk = payFeign.callback(order.getPayId());
        if (callbackOk == null || !callbackOk) {
            throw new BizException("11003", "PAY_FAIL", "支付失败");
        }
        order.setStatus("PAID");
        orderManager.updateById(order);

        // 广播 OrderPaidDTO，供 notify-service 消费
        OrderPaidDTO event = new OrderPaidDTO(order.getId(), order.getUserId(),
            order.getAmount(), LocalDateTime.now().toString());
        eventBus.publish(event);
        log.info("order {} paid, event published", orderId);
        return true;
    }

    @Override
    public String status(Long orderId) {
        OrderPO order = orderManager.getById(orderId);
        return order == null ? "UNKNOWN" : order.getStatus();
    }
}