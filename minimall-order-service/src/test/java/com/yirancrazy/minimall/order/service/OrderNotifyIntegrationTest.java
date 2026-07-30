package com.yirancrazy.minimall.order.service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.dto.pay.PayCreateDTO;
import com.yirancrazy.minimall.api.dto.stock.StockReserveDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;

/**
 * Iter-6 集成测试：验证 LocalEventBus 工作。
 * 测试 order-service 发布 OrderPaidDTO 事件后，Spring 事件监听器能够接收该事件。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(OrderNotifyIntegrationTest.OrderPaidEventCollector.class)
class OrderNotifyIntegrationTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private StockFeignClient stockFeign;

    @MockBean
    private PayFeignClient payFeign;

    @MockBean
    private NotifyFeignClient notifyFeign;

    @Autowired
    private OrderPaidEventCollector eventCollector;

    /**
     * 验证支付订单后，LocalEventBus 发布 OrderPaidDTO 事件，监听器能够接收。
     */
    @Test
    void pay_shouldPublishOrderPaidEvent() {
        // Given: 准备测试数据
        Long userId = 1L;
        Long payId = 8888L;

        // Mock Feign 客户端
        when(stockFeign.reserve(any(StockReserveDTO.class))).thenReturn(true);
        when(payFeign.create(any(PayCreateDTO.class))).thenReturn(payId);
        when(payFeign.callback(payId)).thenReturn(true);
        when(notifyFeign.push(any(NotifyEventDTO.class))).thenReturn(true);

        // When: 创建并支付订单
        Long createdOrderId = orderService.create(userId, 100L, 2);
        boolean payResult = orderService.pay(createdOrderId);

        // Then: 验证支付成功
        assertEquals(true, payResult);

        // 验证事件被发布并捕获（Spring 事件是同步的，无需等待）
        OrderPaidDTO capturedEvent = eventCollector.getLatestEvent();
        assertNotNull(capturedEvent, "OrderPaidDTO event should be published");
        assertNotNull(capturedEvent.getOrderId(), "Event should contain orderId");
        assertEquals(userId, capturedEvent.getUserId(), "Event should contain correct userId");

        // 验证 notifyFeign 被调用
        ArgumentCaptor<NotifyEventDTO> notifyCap = ArgumentCaptor.forClass(NotifyEventDTO.class);
        org.mockito.Mockito.verify(notifyFeign).push(notifyCap.capture());
        assertEquals(userId, notifyCap.getValue().getUserId());
        assertNotNull(notifyCap.getValue().getTitle());
        assertNotNull(notifyCap.getValue().getContent());
    }

    /**
     * 事件收集器：监听 OrderPaidDTO 事件并保存最新事件，供测试断言使用。
     */
    @Component
    static class OrderPaidEventCollector {
        private final AtomicReference<OrderPaidDTO> latestEvent = new AtomicReference<>();

        @EventListener
        public void onOrderPaid(OrderPaidDTO event) {
            latestEvent.set(event);
        }

        OrderPaidDTO getLatestEvent() {
            return latestEvent.get();
        }
    }
}