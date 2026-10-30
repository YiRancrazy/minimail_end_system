package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
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
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OrderManager;
import com.yirancrazy.minimall.order.manager.OutboxManager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单支付状态与发件箱事务一致性集成测试。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@SpringBootTest(properties = {
    "spring.sql.init.schema-locations=classpath:order-outbox-test-schema.sql"
})
@ActiveProfiles("test")
class OrderOutboxTransactionIntegrationTest {

    @MockBean
    private GoodsFeignClient goodsFeignClient;

    @MockBean
    private CartFeignClient cartFeignClient;

    @MockBean
    private CartCleanupFeignClient cartCleanupFeignClient;

    @MockBean
    private StockFeignClient stockFeignClient;

    @MockBean
    private PayFeignClient payFeignClient;

    @MockBean
    private IdFeignClient idFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderManager orderManager;

    @SpyBean
    private OutboxManager outboxManager;

    /**
     * 验证支付状态与发件箱记录在同一事务中提交。
     */
    @Test
    void pay_commitsOrderAndOutboxTogether() {
        Long orderId = 91001L;
        orderManager.save(buildPendingOrder(orderId));

        orderService.pay(orderId);

        OrderPO savedOrder = orderManager.getById(orderId);
        assertEquals(OrderStatusEnum.PAID.intCode(), savedOrder.getStatus());
        List<OutboxPO> events = outboxManager.list(Wrappers.lambdaQuery(OutboxPO.class)
            .eq(OutboxPO::getEventId, "ORDER_PAID:" + orderId));
        assertEquals(1, events.size());
        assertEquals(orderId, events.get(0).getAggregateId());
    }

    /**
     * 验证发件箱写入失败时订单状态同步回滚。
     */
    @Test
    void pay_rollsBackOrderWhenOutboxWriteFails() {
        Long orderId = 91002L;
        orderManager.save(buildPendingOrder(orderId));
        doThrow(new IllegalStateException("outbox failed"))
            .when(outboxManager).save(any(OutboxPO.class));

        assertThrows(RuntimeException.class, () -> orderService.pay(orderId));

        OrderPO savedOrder = orderManager.getById(orderId);
        assertEquals(OrderStatusEnum.PENDING.intCode(), savedOrder.getStatus());
        assertEquals(0, outboxManager.count(Wrappers.lambdaQuery(OutboxPO.class)
            .eq(OutboxPO::getAggregateId, orderId)));
    }

    /**
     * 构造待支付订单。
     *
     * @param orderId 订单 ID
     * @return 待支付订单
     */
    private OrderPO buildPendingOrder(Long orderId) {
        OrderPO order = new OrderPO();
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
