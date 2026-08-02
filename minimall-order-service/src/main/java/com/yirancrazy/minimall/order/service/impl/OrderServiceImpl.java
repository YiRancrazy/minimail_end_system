package com.yirancrazy.minimall.order.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.pay.RefundCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单领域服务实现，编排下单核心链路：取商品快照→锁库存→创建支付流水→持久化订单，并通过事件总线广播订单状态变更。
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderManager orderManager;
    private final GoodsFeignClient goodsFeignClient;
    private final StockFeignClient stockFeignClient;
    private final PayFeignClient payFeignClient;
    private final EventBus eventBus;

    public OrderServiceImpl(OrderManager orderManager,
                            GoodsFeignClient goodsFeignClient,
                            StockFeignClient stockFeignClient,
                            PayFeignClient payFeignClient,
                            EventBus eventBus) {
        this.orderManager = orderManager;
        this.goodsFeignClient = goodsFeignClient;
        this.stockFeignClient = stockFeignClient;
        this.payFeignClient = payFeignClient;
        this.eventBus = eventBus;
    }

    /**
     * 创建订单：取商品快照算金额，Seata AT 全局事务内锁库存与创建支付流水，落库后返回订单ID。
     * @param userId 用户ID
     * @param skuId SKU ID
     * @param quantity 购买数量
     * @return 订单ID
     */
    @Override
    @GlobalTransactional
    public Long create(Long userId, Long skuId, Integer quantity) {
        SkuSnapshotDTO snapshot = goodsFeignClient.skuSnapshot(skuId);
        if (snapshot == null || snapshot.getPrice() == null) {
            throw new BizException(OrderCodeEnum.ORDER_SKU_SNAPSHOT_MISSING);
        }
        BigDecimal amount = snapshot.getPrice().multiply(BigDecimal.valueOf(quantity));

        Boolean reserved = stockFeignClient.reserve(new StockReserveDTO(skuId, quantity));
        if (reserved == null || !reserved) {
            throw new BizException(OrderCodeEnum.STOCK_RESERVE_FAIL);
        }

        OrderPO po = new OrderPO();
        po.setUserId(userId);
        po.setSkuId(skuId);
        po.setQuantity(quantity);
        po.setAmount(amount);
        po.setStatus(OrderStatusEnum.PENDING.intCode());
        orderManager.save(po);

        Long payId = payFeignClient.create(new PayCreateDTO(String.valueOf(po.getId()), userId, 0L, amount));
        if (payId == null || payId < 0) {
            throw new BizException(OrderCodeEnum.ORDER_PAY_CREATE_FAIL);
        }
        po.setPayId(payId);
        orderManager.updateById(po);

        log.info("order created, orderId={}, userId={}, amount={}, payId={}",
            po.getId(), userId, amount, payId);
        return po.getId();
    }

    @Override
    public void pay(Long orderId) {
        OrderPO po = getOrder(orderId);
        OrderPaidDTO event = new OrderPaidDTO(
            orderId, po.getUserId(), po.getAmount(), LocalDateTime.now().toString());
        eventBus.publishInTx(event,
            () -> transitStatus(orderId, OrderStatusEnum.PAID),
            e -> isOrderPaid(orderId));
        log.info("order paid, orderId={}", orderId);
    }

    /**
     * Check whether the order has reached PAID status, used as the
     * transaction-message callback checker.
     * @param orderId the order id to probe
     * @return true if the order is PAID
     */
    private boolean isOrderPaid(Long orderId) {
        OrderPO po = orderManager.getById(orderId);
        return po != null && po.getStatus() == OrderStatusEnum.PAID.intCode();
    }

    @Override
    public void cancel(Long orderId, Long userId) {
        OrderPO po = getOrder(orderId);
        if (!po.getUserId().equals(userId)) {
            throw new BizException(OrderCodeEnum.ORDER_NOT_FOUND);
        }
        transitStatus(orderId, OrderStatusEnum.CANCELLED);
        Boolean released = stockFeignClient.release(
            new StockReserveDTO(po.getSkuId(), po.getQuantity()));
        if (released == null || !released) {
            log.warn("stock release failed on cancel, orderId={}", orderId);
        }
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

        Boolean refunded = payFeignClient.refund(
            new RefundCreateDTO(po.getPayId(), po.getAmount(), null));
        if (refunded == null || !refunded) {
            log.warn("pay refund failed, orderId={}", orderId);
        }
        log.info("order refunding, orderId={}", orderId);
    }

    @Override
    public void handleRefundCallback(Long orderId, boolean success) {
        OrderPO po = getOrder(orderId);
        if (success) {
            po.setStatus(OrderStatusEnum.REFUNDED.intCode());
            log.info("order refunded, orderId={}", orderId);
        }
        else {
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
