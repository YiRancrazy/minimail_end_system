package com.yirancrazy.minimall.order.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.order.OrderPaidDTO;
import com.yirancrazy.minimall.common.event.MqEventJsonCodec;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.order.constant.OrderCodeEnum;
import com.yirancrazy.minimall.order.constant.OutboxSendStatusEnum;
import com.yirancrazy.minimall.order.entity.OutboxPO;
import com.yirancrazy.minimall.order.manager.OutboxManager;
import com.yirancrazy.minimall.order.service.OrderOutboxService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单发件箱服务实现，在现有业务事务中持久化待投递事件。
 * @Version: 1.0
 * @DateTime: 2026/10/30
 **/
@Slf4j
@Service
public class OrderOutboxServiceImpl implements OrderOutboxService {

    private static final String AGGREGATE_TYPE_ORDER = "ORDER";
    private static final String ORDER_PAID_EVENT_PREFIX = "ORDER_PAID:";

    private final OutboxManager outboxManager;
    private final String topic;

    /**
     * 构造订单发件箱服务。
     *
     * @param outboxManager 发件箱管理器
     * @param topic RocketMQ 主题
     */
    public OrderOutboxServiceImpl(
            OutboxManager outboxManager,
            @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
        this.outboxManager = outboxManager;
        this.topic = topic;
    }

    /**
     * 在当前事务中记录订单支付成功事件。
     * 本方法强制要求调用方已开启事务，保证订单状态与事件同时提交或回滚。
     *
     * @param event 订单支付成功事件
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordPaidEvent(OrderPaidDTO event) {
        LocalDateTime now = LocalDateTime.now();
        OutboxPO outbox = new OutboxPO();
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
        }
        catch (DuplicateKeyException e) {
            // 同一业务事件已持久化时保持幂等，后续发布器会继续处理已有记录。
            log.info("订单发件箱事件已存在，eventId={}", outbox.getEventId());
        }
    }
}
