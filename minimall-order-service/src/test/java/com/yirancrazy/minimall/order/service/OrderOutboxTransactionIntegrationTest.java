package com.yirancrazy.minimall.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证订单支付状态与发件箱事件的事务一致性。
 *   业务边界：在 test profile 下使用 H2、真实 OrderService/OrderManager 和 OutboxManager SpyBean。
 *   使用场景：覆盖订单与发件箱同时提交，以及发件箱写入异常时订单回滚。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
@SpringBootTest(
        properties = {"spring.sql.init.schema-locations=classpath:order-outbox-test-schema.sql"})
@ActiveProfiles("test")
class OrderOutboxTransactionIntegrationTest {

    @MockBean private GoodsFeignClient goodsFeignClient;

    @MockBean private CartFeignClient cartFeignClient;

    @MockBean private CartCleanupFeignClient cartCleanupFeignClient;

    @MockBean private StockFeignClient stockFeignClient;

    @MockBean private PayFeignClient payFeignClient;

    @MockBean private IdFeignClient idFeignClient;

    @MockBean private EventBus eventBus;

    @Autowired private OrderService orderService;

    @Autowired private OrderManager orderManager;

    @SpyBean private OutboxManager outboxManager;

    /** 验证支付事务同时提交订单状态和发件箱事件. */
    @Test
    void testPayCommitsOrderAndOutboxTogether() {
        Long orderId = 91001L;
        orderManager.save(buildPendingOrder(orderId));

        orderService.pay(orderId);

        OrderPo savedOrder = orderManager.getById(orderId);
        assertEquals(OrderStatusEnum.PAID.intCode(), savedOrder.getStatus());
        List<OutboxPo> events =
                outboxManager.list(
                        Wrappers.lambdaQuery(OutboxPo.class).eq(OutboxPo::getEventId, "ORDER_PAID:" + orderId));
        assertEquals(1, events.size());
        assertEquals(orderId, events.get(0).getAggregateId());
    }

    /** 验证发件箱写入失败时支付事务回滚订单. */
    @Test
    void testPayRollsBackOrderWhenOutboxWriteFails() {
        Long orderId = 91002L;
        orderManager.save(buildPendingOrder(orderId));
        doThrow(new IllegalStateException("outbox failed"))
                .when(outboxManager)
                .save(any(OutboxPo.class));

        assertThrows(RuntimeException.class, () -> orderService.pay(orderId));

        OrderPo savedOrder = orderManager.getById(orderId);
        assertEquals(OrderStatusEnum.PENDING.intCode(), savedOrder.getStatus());
        assertEquals(
                0,
                outboxManager.count(
                        Wrappers.lambdaQuery(OutboxPo.class).eq(OutboxPo::getAggregateId, orderId)));
    }

    /**
     * 构造待支付订单.
     *
     * @param orderId 订单 ID
     * @return 待支付订单
     */
    private OrderPo buildPendingOrder(Long orderId) {
        OrderPo order = new OrderPo();
        order.setId(orderId);
        order.setOrderNo("OD" + orderId);
        order.setOrderType(1);
        order.setUserId(1L);
        order.setSkuId(100L);
        order.setQuantity(1);
        order.setAmount(new BigDecimal("10.00"));
        order.setTotalAmount(new BigDecimal("10.00"));
        order.setPayAmount(new BigDecimal("10.00"));
        order.setFreightAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayExpireAt(LocalDateTime.now().plusMinutes(30));
        order.setStatus(OrderStatusEnum.PENDING.intCode());
        order.setStockReleaseStatus(0);
        return order;
    }
}
