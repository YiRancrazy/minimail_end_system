package com.yirancrazy.minimall.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证发件箱发布器与 H2 持久化、事件总线桩的协同行为。
 *   业务边界：在 test profile 下加载发件箱 schema，使用真实发布器和 OutboxManager，MockBean 隔离 EventBus。
 *   使用场景：覆盖数据库状态落库、存储路由透传和 MQ 失败后的待重试状态。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
@SpringBootTest(
        properties = {"spring.sql.init.schema-locations=classpath:order-outbox-test-schema.sql"})
@ActiveProfiles("test")
class OrderOutboxPublisherIntegrationTest {

    @MockBean private GoodsFeignClient goodsFeignClient;

    @MockBean private CartFeignClient cartFeignClient;

    @MockBean private CartCleanupFeignClient cartCleanupFeignClient;

    @MockBean private StockFeignClient stockFeignClient;

    @MockBean private PayFeignClient payFeignClient;

    @MockBean private IdFeignClient idFeignClient;

    @MockBean private EventBus eventBus;

    @Autowired private OutboxManager outboxManager;

    @Autowired private OrderOutboxPublisher outboxPublisher;

    /**   清理发件箱数据. */
    @BeforeEach
    void cleanOutbox() {
        outboxManager.remove(Wrappers.lambdaQuery(OutboxPo.class));
    }

    /** 验证发布成功后数据库记录标记为已发送. */
    @Test
    void testPublishPendingMarksDatabaseRecordSent() {
        OutboxPo record = pendingOutbox("ORDER_PAID:92001", 92001L);
        outboxManager.save(record);

        int count = outboxPublisher.publishPending(100);

        OutboxPo saved = outboxManager.getById(record.getId());
        assertEquals(1, count);
        assertEquals(OutboxSendStatusEnum.SENT.intCode(), saved.getSendStatus());
        assertNotNull(saved.getSentTime());
    }

    /** 验证发件箱发布使用已存储的路由. */
    @Test
    void testPublishPendingPassesStoredRouteToEventBus() {
        OutboxPo record = pendingOutbox("ORDER_PAID:92003", 92003L);
        record.setTopic("historical-order-events");
        record.setTag("paid-v1");
        outboxManager.save(record);

        outboxPublisher.publishPending(100);

        ArgumentCaptor<OrderPaidDto> eventCaptor = ArgumentCaptor.forClass(OrderPaidDto.class);
        verify(eventBus).publish(eq("historical-order-events"), eq("paid-v1"), eventCaptor.capture());
        assertEquals(92003L, eventCaptor.getValue().getOrderId());
    }

    /** 验证MQ 失败时保持待处理并增加重试次数. */
    @Test
    void testPublishPendingKeepsPendingAndIncrementsRetryWhenMqFails() {
        OutboxPo record = pendingOutbox("ORDER_PAID:92002", 92002L);
        outboxManager.save(record);
        doThrow(new IllegalStateException("broker down"))
                .when(eventBus)
                .publish(any(), any(), any(OrderPaidDto.class));

        outboxPublisher.publishPending(100);

        OutboxPo saved = outboxManager.getById(record.getId());
        assertEquals(OutboxSendStatusEnum.PENDING.intCode(), saved.getSendStatus());
        assertEquals(1, saved.getRetryCount());
        assertNotNull(saved.getNextRetryTime());
        assertTrue(saved.getLastError().contains("broker down"));
    }

    /**
     * 构造待发送订单支付事件.
     *
     * @param eventId 事件键
     * @param orderId 订单 ID
     * @return 待发送事件
     */
    private OutboxPo pendingOutbox(String eventId, Long orderId) {
        OrderPaidDto event =
                new OrderPaidDto(orderId, 1L, new BigDecimal("10.00"), LocalDateTime.now().toString());
        OutboxPo po = new OutboxPo();
        po.setEventId(eventId);
        po.setEventType("OrderPaidDto");
        po.setAggregateType("ORDER");
        po.setAggregateId(orderId);
        po.setTopic("minimall-events");
        po.setTag("OrderPaidDto");
        po.setPayload(new String(MqEventJsonCodec.encode(event), StandardCharsets.UTF_8));
        po.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
        po.setRetryCount(0);
        po.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        return po;
    }
}
