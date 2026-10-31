package com.yirancrazy.minimall.order.manager.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.api.feign.GoodsFeignClient;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.api.feign.PayFeignClient;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱状态条件更新集成测试。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@SpringBootTest(properties = {
    "spring.sql.init.schema-locations=classpath:order-outbox-test-schema.sql"
})
@ActiveProfiles("test")
class OutboxManagerStateIntegrationTest {

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

    /**
     * 清理发件箱数据。
     */
    @BeforeEach
    void cleanOutbox() {
        outboxManager.remove(Wrappers.lambdaQuery(OutboxPO.class));
    }

    /**
     * 验证同一事件只有一个调用可以抢占成功。
     */
    @Test
    void claimForDispatch_allowsOnlyOneWinner() {
        OutboxPO po = pendingOutbox("ORDER_PAID:1", 1L);
        outboxManager.save(po);
        LocalDateTime now = LocalDateTime.now();

        boolean first = outboxManager.claimForDispatch(
            po.getId(), now, now.plusSeconds(30));
        boolean second = outboxManager.claimForDispatch(
            po.getId(), now, now.plusSeconds(30));

        assertTrue(first);
        assertFalse(second);
    }

    /**
     * 验证扫描只返回已到期且存在 payload 的待发送事件。
     */
    @Test
    void listDispatchCandidates_filtersInvalidRecords() {
        OutboxPO ready = pendingOutbox("ORDER_PAID:2", 2L);
        outboxManager.save(ready);
        OutboxPO future = pendingOutbox("ORDER_PAID:3", 3L);
        future.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
        outboxManager.save(future);
        OutboxPO noPayload = pendingOutbox("ORDER_PAID:4", 4L);
        noPayload.setPayload(null);
        outboxManager.save(noPayload);

        List<OutboxPO> records = outboxManager.listDispatchCandidates(LocalDateTime.now(), 100);

        assertEquals(1, records.size());
        assertEquals(ready.getId(), records.get(0).getId());
    }

    /**
     * 验证发送成功、重试和死信状态更新。
     */
    @Test
    void stateUpdates_persistExpectedValues() {
        OutboxPO sent = pendingOutbox("ORDER_PAID:5", 5L);
        outboxManager.save(sent);
        outboxManager.markSent(sent.getId(), LocalDateTime.now());
        OutboxPO savedSent = outboxManager.getById(sent.getId());
        assertEquals(OutboxSendStatusEnum.SENT.intCode(), savedSent.getSendStatus());
        assertNotNull(savedSent.getSentTime());

        OutboxPO retry = pendingOutbox("ORDER_PAID:6", 6L);
        outboxManager.save(retry);
        LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(5);
        outboxManager.markRetry(retry.getId(), 1, nextRetryTime, "mq down");
        OutboxPO savedRetry = outboxManager.getById(retry.getId());
        assertEquals(OutboxSendStatusEnum.PENDING.intCode(), savedRetry.getSendStatus());
        assertEquals(1, savedRetry.getRetryCount());
        assertEquals("mq down", savedRetry.getLastError());

        OutboxPO dead = pendingOutbox("ORDER_PAID:7", 7L);
        outboxManager.save(dead);
        outboxManager.markDead(dead.getId(), 10, "max retry exceeded");
        OutboxPO savedDead = outboxManager.getById(dead.getId());
        assertEquals(OutboxSendStatusEnum.DEAD.intCode(), savedDead.getSendStatus());
        assertEquals(10, savedDead.getRetryCount());
    }

    /**
     * 构造待发送事件。
     *
     * @param eventId 事件键
     * @param orderId 订单 ID
     * @return 待发送事件
     */
    private OutboxPO pendingOutbox(String eventId, Long orderId) {
        OutboxPO po = new OutboxPO();
        po.setEventId(eventId);
        po.setEventType("OrderPaidDTO");
        po.setAggregateType("ORDER");
        po.setAggregateId(orderId);
        po.setTopic("minimall-events");
        po.setTag("OrderPaidDTO");
        po.setPayload("{}");
        po.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
        po.setRetryCount(0);
        po.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
        return po;
    }
}
