package com.yirancrazy.minimall.order.manager.impl;

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
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证发件箱候选筛选、条件抢占及状态更新的数据库语义。
 *   业务边界：在 test profile 下加载 H2 发件箱 schema，使用真实 OutboxManager 并隔离 Feign 与事件总线。
 *   使用场景：覆盖并发抢占唯一胜者、无效候选过滤及 SENT/RETRY/DEAD 持久化。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
@SpringBootTest(
    properties = {"spring.sql.init.schema-locations=classpath:order-outbox-test-schema.sql"})
@ActiveProfiles("test")
class OutboxManagerStateIntegrationTest {

  @MockBean private GoodsFeignClient goodsFeignClient;

  @MockBean private CartFeignClient cartFeignClient;

  @MockBean private CartCleanupFeignClient cartCleanupFeignClient;

  @MockBean private StockFeignClient stockFeignClient;

  @MockBean private PayFeignClient payFeignClient;

  @MockBean private IdFeignClient idFeignClient;

  @MockBean private EventBus eventBus;

  @Autowired private OutboxManager outboxManager;

  /**   清理发件箱数据. */
  @BeforeEach
  void cleanOutbox() {
    outboxManager.remove(Wrappers.lambdaQuery(OutboxPo.class));
  }

  /** 验证同一发件箱记录并发抢占仅一个实例成功. */
  @Test
  void testClaimForDispatchAllowsOnlyOneWinner() {
    OutboxPo po = pendingOutbox("ORDER_PAID:1", 1L);
    outboxManager.save(po);
    LocalDateTime now = LocalDateTime.now();

    boolean first = outboxManager.claimForDispatch(po.getId(), now, now.plusSeconds(30));
    boolean second = outboxManager.claimForDispatch(po.getId(), now, now.plusSeconds(30));

    assertTrue(first);
    assertFalse(second);
  }

  /** 验证分发候选查询过滤未到期和无载荷记录. */
  @Test
  void testListDispatchCandidatesFiltersInvalidRecords() {
    OutboxPo ready = pendingOutbox("ORDER_PAID:2", 2L);
    outboxManager.save(ready);
    OutboxPo future = pendingOutbox("ORDER_PAID:3", 3L);
    future.setNextRetryTime(LocalDateTime.now().plusMinutes(1));
    outboxManager.save(future);
    OutboxPo noPayload = pendingOutbox("ORDER_PAID:4", 4L);
    noPayload.setPayload(null);
    outboxManager.save(noPayload);

    List<OutboxPo> records = outboxManager.listDispatchCandidates(LocalDateTime.now(), 100);

    assertEquals(1, records.size());
    assertEquals(ready.getId(), records.get(0).getId());
  }

  /** 验证状态更新持久化预期值. */
  @Test
  void testStateUpdatesPersistExpectedValues() {
    OutboxPo sent = pendingOutbox("ORDER_PAID:5", 5L);
    outboxManager.save(sent);
    outboxManager.markSent(sent.getId(), LocalDateTime.now());
    OutboxPo savedSent = outboxManager.getById(sent.getId());
    assertEquals(OutboxSendStatusEnum.SENT.intCode(), savedSent.getSendStatus());
    assertNotNull(savedSent.getSentTime());

    OutboxPo retry = pendingOutbox("ORDER_PAID:6", 6L);
    outboxManager.save(retry);
    LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(5);
    outboxManager.markRetry(retry.getId(), 1, nextRetryTime, "mq down");
    OutboxPo savedRetry = outboxManager.getById(retry.getId());
    assertEquals(OutboxSendStatusEnum.PENDING.intCode(), savedRetry.getSendStatus());
    assertEquals(1, savedRetry.getRetryCount());
    assertEquals("mq down", savedRetry.getLastError());

    OutboxPo dead = pendingOutbox("ORDER_PAID:7", 7L);
    outboxManager.save(dead);
    outboxManager.markDead(dead.getId(), 10, "max retry exceeded");
    OutboxPo savedDead = outboxManager.getById(dead.getId());
    assertEquals(OutboxSendStatusEnum.DEAD.intCode(), savedDead.getSendStatus());
    assertEquals(10, savedDead.getRetryCount());
  }

  /**
   * 构造待发送事件.
   *
   * @param eventId 事件键
   * @param orderId 订单 ID
   * @return 待发送事件
   */
  private OutboxPo pendingOutbox(String eventId, Long orderId) {
    OutboxPo po = new OutboxPo();
    po.setEventId(eventId);
    po.setEventType("OrderPaidDto");
    po.setAggregateType("ORDER");
    po.setAggregateId(orderId);
    po.setTopic("minimall-events");
    po.setTag("OrderPaidDto");
    po.setPayload("{}");
    po.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
    po.setRetryCount(0);
    po.setNextRetryTime(LocalDateTime.now().minusSeconds(1));
    return po;
  }
}
