package com.yirancrazy.minimall.order.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.order.constant.OrderStatusEnum;

/**
 * 订单服务集成测试：验证订单状态机流转逻辑。
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderNotifyIntegrationTest {

    @Autowired
    private OrderService orderService;

    /**
     * 验证创建订单后状态为 PENDING，支付后状态为 PAID。
     */
    @Test
    void createAndPay_shouldTransitionStatus() {
        Long userId = 1L;
        Long skuId = 100L;
        Integer quantity = 2;

        Long orderId = orderService.create(userId, skuId, quantity);
        assertNotNull(orderId, "orderId should not be null");
        assertEquals(OrderStatusEnum.PENDING.intCode(), orderService.getStatus(orderId));

        orderService.pay(orderId);
        assertEquals(OrderStatusEnum.PAID.intCode(), orderService.getStatus(orderId));
    }
}
