package com.yirancrazy.minimall.order.service.impl;

import com.yirancrazy.minimall.api.dto.order.OrderPaidDto;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPo;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.OrderOutboxService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 实现订单支付事件到事务性发件箱记录的转换与持久化。
 *   业务边界：强制调用方已开启事务，使用稳定事件 ID 去重，并写入 topic、tag、JSON 载荷和首次重试时间。
 *   使用场景：OrderServiceImpl 在支付状态推进的同一本地事务中调用。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 */
@Slf4j
@Service
public class OrderOutboxServiceImpl implements OrderOutboxService {

  private static final String AGGREGATE_TYPE_ORDER = "ORDER";
  private static final String ORDER_PAID_EVENT_PREFIX = "ORDER_PAID:";

  private final OutboxManager outboxManager;
  private final String topic;

  /**
   * 创建订单发件箱服务。
   *
   * <p>注入发件箱管理器与默认事件 topic，topic 可由配置覆盖。
   *
   * @param outboxManager 发件箱记录持久化与条件更新入口
   * @param topic 订单事件默认 RocketMQ 主题
   */
  public OrderOutboxServiceImpl(
      OutboxManager outboxManager,
      @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
    this.outboxManager = outboxManager;
    this.topic = topic;
  }

  /**
   * 在当前事务中记录订单支付成功事件。 本方法强制要求调用方已开启事务，保证订单状态与事件同时提交或回滚.
   *
   * @param event 订单支付成功事件
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void recordPaidEvent(OrderPaidDto event) {
    LocalDateTime now = LocalDateTime.now();
    OutboxPo outbox = new OutboxPo();
    outbox.setEventId(ORDER_PAID_EVENT_PREFIX + event.getOrderId());
    outbox.setEventType(event.getClass().getSimpleName());
    outbox.setAggregateType(AGGREGATE_TYPE_ORDER);
    outbox.setAggregateId(event.getOrderId());
    outbox.setTopic(topic);
    outbox.setTag(MqEventJsonCodec.tagFor(event.getClass()));
    outbox.setPayload(new String(MqEventJsonCodec.encode(event), StandardCharsets.UTF_8));
    outbox.setSendStatus(OutboxSendStatusEnum.PENDING.intCode());
    outbox.setRetryCount(0);
    outbox.setNextRetryTime(now);
    try {
      if (!outboxManager.save(outbox)) {
        throw new BizException(OrderCodeEnum.ORDER_OUTBOX_SAVE_FAILED);
      }
    } catch (DuplicateKeyException e) {
      // 同一业务事件已持久化时保持幂等，后续发布器会继续处理已有记录。
      log.info("订单发件箱事件已存在，eventId={}", outbox.getEventId());
    }
  }
}
