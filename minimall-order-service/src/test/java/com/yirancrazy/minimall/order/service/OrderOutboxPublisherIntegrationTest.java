package com.yirancrazy.minimall.order.service;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱发布器集成测试。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@SpringBootTest(properties = {
    "spring.sql.init.schema-locations=classpath:order-outbox-test-schema.sql"
})
@ActiveProfiles("test")
class OrderOutboxPublisherIntegrationTest {

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

    @MockBean
    private EventBus eventBus;

    @Autowired
    private OutboxManager outboxManager;

    @Autowired
    private OrderOutboxPublisher outboxPublisher;

    /**
     * 清理发件箱数据。
     */
    @BeforeEach
    void cleanOutbox() {
        outboxManager.remove(Wrappers.lambdaQuery(OutboxPO.class));
    }

    /**
     * 验证发布成功后数据库状态为已发送。
     */
    @Test
    void publishPending_marksDatabaseRecordSent() {
        OutboxPO record = pendingOutbox("ORDER_PAID:92001", 92001L);
        outboxManager.save(record);

        int count = outboxPublisher.publishPending(100);

        OutboxPO saved = outboxManager.getById(record.getId());
        assertEquals(1, count);
        assertEquals(OutboxSendStatusEnum.SENT.intCode(), saved.getSendStatus());
        assertNotNull(saved.getSentTime());
    }

    /**
     * 验证数据库记录中的路由被传递给事件总线。
     */
    @Test
    void publishPending_passesStoredRouteToEventBus() {
        OutboxPO record = pendingOutbox("ORDER_PAID:92003", 92003L);
        record.setTopic("historical-order-events");
        record.setTag("paid-v1");
        outboxManager.save(record);

        outboxPublisher.publishPending(100);

        ArgumentCaptor<OrderPaidDTO> eventCaptor = ArgumentCaptor.forClass(OrderPaidDTO.class);
        verify(eventBus).publish(
            eq("historical-order-events"), eq("paid-v1"), eventCaptor.capture());
        assertEquals(92003L, eventCaptor.getValue().getOrderId());
    }

    /**
     * 验证 MQ 失败后记录保持待发送并增加重试次数。
     */
    @Test
    void publishPending_keepsPendingAndIncrementsRetryWhenMqFails() {
        OutboxPO record = pendingOutbox("ORDER_PAID:92002", 92002L);
        outboxManager.save(record);
        doThrow(new IllegalStateException("broker down"))
            .when(eventBus).publish(any(), any(), any(OrderPaidDTO.class));

        outboxPublisher.publishPending(100);

        OutboxPO saved = outboxManager.getById(record.getId());
        assertEquals(OutboxSendStatusEnum.PENDING.intCode(), saved.getSendStatus());
        assertEquals(1, saved.getRetryCount());
        assertNotNull(saved.getNextRetryTime());
        assertTrue(saved.getLastError().contains("broker down"));
    }

    /**
     * 构造待发送订单支付事件。
     *
     * @param eventId 事件键
     * @param orderId 订单 ID
     * @return 待发送事件
     */
    private OutboxPO pendingOutbox(String eventId, Long orderId) {
        OrderPaidDTO event = new OrderPaidDTO(
            orderId, 1L, new BigDecimal("10.00"), LocalDateTime.now().toString());
        OutboxPO po = new OutboxPO();
        po.setEventId(eventId);
        po.setEventType("OrderPaidDTO");
        po.setAggregateType("ORDER");
        po.setAggregateId(orderId);
        po.setTopic("minimall-events");
        po.setTag("OrderPaidDTO");
        po.setPayload(new String(MqEventJsonCodec.encode(event), StandardCharsets.UTF_8));
        po.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
        po.setRetryCount(0);
        po.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        return po;
    }
}
