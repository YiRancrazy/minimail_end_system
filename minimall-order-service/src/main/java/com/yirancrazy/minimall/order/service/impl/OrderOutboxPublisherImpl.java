package com.yirancrazy.minimall.order.service.impl;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现订单发件箱事件的数据库抢占、RocketMQ 发布和失败状态推进。
 *   业务边界：单次最多扫描配置批数，使用租约避免并发重复发送，并按指数退避推进重试或死信。
 *   使用场景：Spring Scheduling 或 XXL-Job 的订单发件箱转发任务调用。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
@Slf4j
@Service
public class OrderOutboxPublisherImpl implements OrderOutboxPublisher {

  private static final String ORDER_PAID_EVENT_TYPE = "OrderPaidDto";
  private static final int MAX_ERROR_LENGTH = 512;

  private final OutboxManager outboxManager;
  private final EventBus eventBus;
  private final int maxBatches;
  private final int leaseSeconds;
  private final int maxRetryCount;
  private final long retryBaseSeconds;
  private final long retryMaxSeconds;

  /**
   * 创建订单发件箱发布器。
   *
   * <p>注入发件箱管理器、事件总线和批次数、租约、重试上限及退避参数。
   *
   * @param outboxManager 发件箱记录持久化与条件更新入口
   * @param eventBus 订单事件投递总线
   * @param maxBatches 单轮最多扫描的批次数
   * @param leaseSeconds 事件发送租约秒数
   * @param maxRetryCount 事件进入死信前的最大重试次数
   * @param retryBaseSeconds 指数退避的基础秒数
   * @param retryMaxSeconds 指数退避的最大秒数
   */
  public OrderOutboxPublisherImpl(
      OutboxManager outboxManager,
      EventBus eventBus,
      @Value("${minimall.outbox.max-batches:10}") int maxBatches,
      @Value("${minimall.outbox.lease-seconds:30}") int leaseSeconds,
      @Value("${minimall.outbox.max-retry-count:10}") int maxRetryCount,
      @Value("${minimall.outbox.retry-base-seconds:5}") long retryBaseSeconds,
      @Value("${minimall.outbox.retry-max-seconds:300}") long retryMaxSeconds) {
    this.outboxManager = outboxManager;
    this.eventBus = eventBus;
    this.maxBatches = maxBatches;
    this.leaseSeconds = leaseSeconds;
    this.maxRetryCount = maxRetryCount;
    this.retryBaseSeconds = retryBaseSeconds;
    this.retryMaxSeconds = retryMaxSeconds;
  }

  /**
   * 批量发布到期的订单发件箱事件。
   *
   * <p>每批先查询候选并用数据库条件抢占，投递成功标记已发送，失败按指数退避重试或进入死信；返回仅统计成功发送数。
   *
   * @param limit 单批最大处理数量
   * @return 本轮成功发送的事件数量。
   */
  @Override
  public int publishPending(int limit) {
    int sent = 0;
    int retried = 0;
    int dead = 0;
    int scanned = 0;
    LocalDateTime oldestPendingTime = null;
    for (int batch = 0; batch < maxBatches; batch++) {
      LocalDateTime now = LocalDateTime.now();
      List<OutboxPo> records = outboxManager.listDispatchCandidates(now, limit);
      if (records.isEmpty()) {
        break;
      }
      scanned += records.size();
      for (OutboxPo record : records) {
        if (oldestPendingTime == null || record.getNextRetryTime().isBefore(oldestPendingTime)) {
          oldestPendingTime = record.getNextRetryTime();
        }
        LocalDateTime claimTime = LocalDateTime.now();
        if (!outboxManager.claimForDispatch(
            record.getId(), claimTime, claimTime.plusSeconds(leaseSeconds))) {
          continue;
        }
        PublishResult result = publishOne(record);
        if (result == PublishResult.SENT) {
          sent++;
        } else if (result == PublishResult.RETRIED) {
          retried++;
        } else {
          dead++;
        }
      }
      if (records.size() < limit) {
        break;
      }
    }
    if (scanned > 0) {
      log.info(
          "订单发件箱发布完成，scanned={}，sent={}，retried={}，dead={}，oldestPendingTime={}",
          scanned,
          sent,
          retried,
          dead,
          oldestPendingTime);
    }
    return sent;
  }

  /**
   * 处理单条已抢占的发件箱事件。
   *
   * <p>先解码并校验路由，再发布到事件总线；成功标记已发送，不可解析或路由缺失直接死信，发布异常按重试策略处理。
   *
   * @param record 发件箱记录
   * @return 单条事件的发布结果。
   */
  private PublishResult publishOne(OutboxPo record) {
    OrderPaidDto event;
    try {
      event = decodePaidEvent(record);
    } catch (RuntimeException e) {
      String error = truncateError(e);
      outboxManager.markDead(record.getId(), retryCount(record), error);
      log.error(
          "订单发件箱事件不可解析，eventId={}，eventType={}", record.getEventId(), record.getEventType(), e);
      return PublishResult.DEAD;
    }
    if (record.getTopic() == null
        || record.getTopic().isBlank()
        || record.getTag() == null
        || record.getTag().isBlank()) {
      String error = "missing outbox route: topic/tag";
      outboxManager.markDead(record.getId(), retryCount(record), error);
      log.error(
          "订单发件箱事件路由缺失，eventId={}，topic={}，tag={}",
          record.getEventId(),
          record.getTopic(),
          record.getTag());
      return PublishResult.DEAD;
    }
    try {
      eventBus.publish(record.getTopic(), record.getTag(), event);
      outboxManager.markSent(record.getId(), LocalDateTime.now());
      log.debug(
          "订单发件箱事件发送成功，eventId={}，topic={}，tag={}",
          record.getEventId(),
          record.getTopic(),
          record.getTag());
      return PublishResult.SENT;
    } catch (Exception e) {
      return handlePublishFailure(record, e);
    }
  }

  /**
   * 处理消息发布失败.
   *
   * @param record 发件箱记录
   * @param exception 发布异常
   * @return 发布结果
   */
  private PublishResult handlePublishFailure(OutboxPo record, Exception exception) {
    int nextRetryCount = retryCount(record) + 1;
    String error = truncateError(exception);
    if (nextRetryCount >= maxRetryCount) {
      outboxManager.markDead(record.getId(), nextRetryCount, error);
      log.error(
          "订单发件箱事件达到重试上限，eventId={}，retryCount={}", record.getEventId(), nextRetryCount, exception);
      return PublishResult.DEAD;
    }
    LocalDateTime nextRetryTime =
        LocalDateTime.now().plusSeconds(calculateDelaySeconds(nextRetryCount));
    outboxManager.markRetry(record.getId(), nextRetryCount, nextRetryTime, error);
    log.warn(
        "订单发件箱事件发送失败，eventId={}，retryCount={}，nextRetryTime={}",
        record.getEventId(),
        nextRetryCount,
        nextRetryTime,
        exception);
    return PublishResult.RETRIED;
  }

  /**
   * 把发件箱记录解码为订单支付事件。
   *
   * <p>只接受 OrderPaidDto 和非空 JSON 载荷；不支持的类型或损坏载荷会导致当前事件进入死信。
   *
   * @param record 发件箱记录
   * @return 解码后的支付成功事件。
   * @throws BizException 事件类型不支持或载荷为空/损坏时抛出运行时异常。
   */
  private OrderPaidDto decodePaidEvent(OutboxPo record) {
    if (!ORDER_PAID_EVENT_TYPE.equals(record.getEventType())) {
      throw new IllegalArgumentException("不支持的事件类型：" + record.getEventType());
    }
    if (record.getPayload() == null || record.getPayload().isBlank()) {
      throw new IllegalArgumentException("事件内容为空");
    }
    return MqEventJsonCodec.decode(
        record.getPayload().getBytes(StandardCharsets.UTF_8), OrderPaidDto.class);
  }

  /**
   * 计算指数退避秒数.
   *
   * @param retryCount 重试次数
   * @return 退避秒数
   */
  private long calculateDelaySeconds(int retryCount) {
    int exponent = Math.min(Math.max(retryCount - 1, 0), 20);
    long multiplier = 1L << exponent;
    return Math.min(retryBaseSeconds * multiplier, retryMaxSeconds);
  }

  /**
   * 读取发件箱记录的非空重试次数。
   *
   * <p>数据库字段为空时按 0 处理，避免发布器空指针。
   *
   * @param record 发件箱记录
   * @return 当前重试次数。
   */
  private int retryCount(OutboxPo record) {
    return record.getRetryCount() == null ? 0 : record.getRetryCount();
  }

  /**
   * 截断错误信息.
   *
   * @param exception 异常
   * @return 截断后的错误信息
   */
  private String truncateError(Exception exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      message = exception.getClass().getSimpleName();
    }
    return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 表示单条发件箱事件的本次处理结果。
   *   业务边界：SENT 表示已发送，RETRIED 表示等待下次重试，DEAD 表示不可解析、路由缺失或达到重试上限。
   *   使用场景：仅用于 OrderOutboxPublisherImpl 的批量计数和日志汇总。
   * @Version: 1.0
   * @DateTime: 2026/10/31
   */
  private enum PublishResult {
    SENT,
    RETRIED,
    DEAD
  }
}
