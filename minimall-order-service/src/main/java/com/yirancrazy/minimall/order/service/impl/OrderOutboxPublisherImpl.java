package com.yirancrazy.minimall.order.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.event.EventBus;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.OrderOutboxPublisher;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱发布器实现，通过数据库条件抢占安全发送待处理事件。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@Slf4j
@Service
public class OrderOutboxPublisherImpl implements OrderOutboxPublisher {

    private static final String ORDER_PAID_EVENT_TYPE = "OrderPaidDTO";
    private static final int MAX_ERROR_LENGTH = 512;

    private final OutboxManager outboxManager;
    private final EventBus eventBus;
    private final int maxBatches;
    private final int leaseSeconds;
    private final int maxRetryCount;
    private final long retryBaseSeconds;
    private final long retryMaxSeconds;

    /**
     * 构造订单发件箱发布器。
     *
     * @param outboxManager 发件箱管理器
     * @param eventBus 事件总线
     * @param maxBatches 单轮最大批次数
     * @param leaseSeconds 发送租约秒数
     * @param maxRetryCount 最大重试次数
     * @param retryBaseSeconds 基础退避秒数
     * @param retryMaxSeconds 最大退避秒数
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
     * 发布待处理发件箱事件。
     *
     * @param limit 单批处理数量
     * @return 成功发布的事件数量
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
            List<OutboxPO> records = outboxManager.listDispatchCandidates(now, limit);
            if (records.isEmpty()) {
                break;
            }
            scanned += records.size();
            for (OutboxPO record : records) {
                if (oldestPendingTime == null
                    || record.getNextRetryTime().isBefore(oldestPendingTime)) {
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
                }
                else if (result == PublishResult.RETRIED) {
                    retried++;
                }
                else {
                    dead++;
                }
            }
            if (records.size() < limit) {
                break;
            }
        }
        if (scanned > 0) {
            log.info("订单发件箱发布完成，scanned={}，sent={}，retried={}，dead={}，oldestPendingTime={}",
                scanned, sent, retried, dead, oldestPendingTime);
        }
        return sent;
    }

    /**
     * 发布单条事件。
     *
     * @param record 发件箱记录
     * @return 发布结果
     */
    private PublishResult publishOne(OutboxPO record) {
        OrderPaidDTO event;
        try {
            event = decodePaidEvent(record);
        }
        catch (RuntimeException e) {
            String error = truncateError(e);
            outboxManager.markDead(record.getId(), retryCount(record), error);
            log.error("订单发件箱事件不可解析，eventId={}，eventType={}",
                record.getEventId(), record.getEventType(), e);
            return PublishResult.DEAD;
        }
        try {
            eventBus.publish(event);
            outboxManager.markSent(record.getId(), LocalDateTime.now());
            log.debug("订单发件箱事件发送成功，eventId={}", record.getEventId());
            return PublishResult.SENT;
        }
        catch (Exception e) {
            return handlePublishFailure(record, e);
        }
    }

    /**
     * 处理消息发布失败。
     *
     * @param record 发件箱记录
     * @param exception 发布异常
     * @return 发布结果
     */
    private PublishResult handlePublishFailure(OutboxPO record, Exception exception) {
        int nextRetryCount = retryCount(record) + 1;
        String error = truncateError(exception);
        if (nextRetryCount >= maxRetryCount) {
            outboxManager.markDead(record.getId(), nextRetryCount, error);
            log.error("订单发件箱事件达到重试上限，eventId={}，retryCount={}",
                record.getEventId(), nextRetryCount, exception);
            return PublishResult.DEAD;
        }
        LocalDateTime nextRetryTime = LocalDateTime.now()
            .plusSeconds(calculateDelaySeconds(nextRetryCount));
        outboxManager.markRetry(record.getId(), nextRetryCount, nextRetryTime, error);
        log.warn("订单发件箱事件发送失败，eventId={}，retryCount={}，nextRetryTime={}",
            record.getEventId(), nextRetryCount, nextRetryTime, exception);
        return PublishResult.RETRIED;
    }

    /**
     * 解码订单支付事件。
     *
     * @param record 发件箱记录
     * @return 订单支付事件
     */
    private OrderPaidDTO decodePaidEvent(OutboxPO record) {
        if (!ORDER_PAID_EVENT_TYPE.equals(record.getEventType())) {
            throw new IllegalArgumentException("不支持的事件类型：" + record.getEventType());
        }
        if (record.getPayload() == null || record.getPayload().isBlank()) {
            throw new IllegalArgumentException("事件内容为空");
        }
        return MqEventJsonCodec.decode(
            record.getPayload().getBytes(StandardCharsets.UTF_8), OrderPaidDTO.class);
    }

    /**
     * 计算指数退避秒数。
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
     * 获取安全的重试次数。
     *
     * @param record 发件箱记录
     * @return 重试次数
     */
    private int retryCount(OutboxPO record) {
        return record.getRetryCount() == null ? 0 : record.getRetryCount();
    }

    /**
     * 截断错误信息。
     *
     * @param exception 异常
     * @return 截断后的错误信息
     */
    private String truncateError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH
            ? message : message.substring(0, MAX_ERROR_LENGTH);
    }

    /**
     * 单条事件发布结果。
     */
    private enum PublishResult {
        SENT,
        RETRIED,
        DEAD
    }
}
