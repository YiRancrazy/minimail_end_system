package com.yirancrazy.minimall.common.event;

import java.util.function.Consumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.mq.RocketMqTransactionRollbackException;
import com.yirancrazy.minimall.common.exception.mq.RocketMqTransactionUnknownException;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 基于 RocketMQ 事务消息的事件总线实现。
 *               普通事件直接发送；事务事件通过本地事务回调提交，并通过 OutboxStore 完成状态回查。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
public class RocketMqEventBus implements EventBus {

    private final String namesrvAddr;
    private final String topic;
    private TransactionMQProducer producer;

    private OutboxStore outboxStore;

    /**
     * 创建 RocketMQ 事件总线。
     *
     * @param namesrvAddr RocketMQ NameServer 地址
     * @param topic 事件消息主题
     */
    public RocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }


    /**
     * 设置本地消息表存储，用于事务消息回查。
     *
     * @param outboxStore 本地消息表存储
     */
    public void setOutboxStore(OutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    /**
     * 创建并启动 RocketMQ 事务生产者。
     *
     * @throws IllegalStateException 生产者启动失败时抛出
     */
    @PostConstruct
    void start() {
        producer = new TransactionMQProducer("minimall-producer");
        producer.setNamesrvAddr(namesrvAddr);
        producer.setTransactionListener(new TransactionListener() {
            /**
             * 执行本地事务，并决定事务消息的初始状态。
             *
             * @param msg RocketMQ 消息
             * @param arg 本地事务回调
             * @return 事务消息状态
             */
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                @SuppressWarnings("unchecked")
                Consumer<String> localTx = (Consumer<String>) arg;
                try {
                    localTx.accept(msg.getTransactionId());
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                catch (Exception e) {
                    log.warn("本地事务执行失败，tag={}: {}", msg.getTags(), e.getMessage());
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }

            /**
             * 回查本地事务状态。
             * 仅使用共享 OutboxStore，避免依赖发送实例的进程内状态。
             *
             * @param msg RocketMQ 消息
             * @return 事务消息状态；无法判断时返回 UNKNOW
             */
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String transactionId = msg.getTransactionId();
                if (transactionId == null || transactionId.isBlank()) {
                    log.warn("transactionId 为空，无法执行事务回查，tag={}", msg.getTags());
                    return LocalTransactionState.UNKNOW;
                }
                if (outboxStore == null) {
                    log.error("OutboxStore 未配置，无法执行事务回查，transactionId={}，tag={}",
                        transactionId, msg.getTags());
                    return LocalTransactionState.UNKNOW;
                }
                try {
                    return toLocalTransactionState(outboxStore.commitStatus(transactionId));
                }
                catch (Exception e) {
                    log.warn("outbox 状态检查失败，transactionId={}，tag={}: {}",
                        transactionId, msg.getTags(), e.getMessage());
                    return LocalTransactionState.UNKNOW;
                }
            }
        });
        try {
            producer.start();
            log.info("rocketmq 事务生产者已启动，namesrv={}，topic={}", namesrvAddr, topic);
        }
        catch (MQClientException e) {
            log.error("rocketmq 生产者启动失败", e);
            producer = null;
            throw new IllegalStateException("RocketMQ 生产者启动失败", e);
        }
    }

    /**
     * 关闭 RocketMQ 事务生产者。
     */
    @PreDestroy
    void stop() {
        if (producer != null) {
            producer.shutdown();
        }
    }


    /**
     * 立即发送事件消息，不绑定本地事务。
     *
     * @param event 待发布的事件对象
     * @throws IllegalStateException 生产者未启动或消息发送失败时抛出
     */
    public void publish(Object event) {
        if (producer == null) {
            throw new IllegalStateException("RocketMQ 生产者未启动");
        }
        try {
            Message msg = new Message(topic, MqEventJsonCodec.tagFor(event.getClass()),
                MqEventJsonCodec.encode(event));
            attachTraceId(msg);
            producer.send(msg);
            log.debug("rocketmq 发送成功，event={}", event.getClass().getSimpleName());
        }
        catch (Exception e) {
            log.error("rocketmq 发送失败，event={}", event.getClass().getSimpleName(), e);
            throw new IllegalStateException("RocketMQ 消息发送失败", e);
        }
    }


    /**
     * 发送事务消息。
     *
     * @param event 待发布的事件对象
     * @param localTx 本地事务回调，接收 RocketMQ 事务 ID
     * @throws IllegalStateException 生产者未启动、OutboxStore 未配置或事务消息发送失败时抛出
     * @throws RocketMqTransactionRollbackException 本地事务回滚时抛出
     * @throws RocketMqTransactionUnknownException 本地事务状态未知时抛出
     */
    public void publishInTx(Object event, Consumer<String> localTx) {
        if (producer == null) {
            throw new IllegalStateException("RocketMQ 生产者未启动");
        }
        if (outboxStore == null) {
            throw new IllegalStateException("RocketMQ 事务消息必须配置 OutboxStore");
        }
        String tag = MqEventJsonCodec.tagFor(event.getClass());
        TransactionSendResult transactionSendResult;

        // 1. 半消息发送阶段
        try {
            Message msg = new Message(topic, tag, MqEventJsonCodec.encode(event));
            attachTraceId(msg);
            transactionSendResult = producer.sendMessageInTransaction(msg, localTx);
            log.debug("rocketmq 事务消息发送成功，event={}，state={}", event.getClass().getSimpleName(),
                transactionSendResult.getLocalTransactionState());
        }
        catch (Exception e) {
            log.error("rocketmq 事务消息发送失败，event={}", event.getClass().getSimpleName(), e);
            throw new IllegalStateException("RocketMQ 事务消息发送失败", e);
        }

        LocalTransactionState localTransactionState = transactionSendResult.getLocalTransactionState();
        if(localTransactionState == null){
            log.warn("rocketmq 事务消息状态未知，等待 broker 通过 Outbox 回查，event={}",
                event.getClass().getSimpleName());
            throw new RocketMqTransactionUnknownException(tag, event, null);
        }

        switch (localTransactionState) {
            case COMMIT_MESSAGE:
                log.debug("rocketmq 事务消息本地结果成功，event={}，state={}", event.getClass().getSimpleName(),
                    localTransactionState);
                break;
            case ROLLBACK_MESSAGE:
                log.debug("rocketmq 事务消息本地结果失败，event={}，state={}", event.getClass().getSimpleName(),
                    localTransactionState);
                throw new RocketMqTransactionRollbackException(tag, event, null);
            case UNKNOW:
                log.warn("rocketmq 事务消息状态未知，等待 broker 通过 Outbox 回查，event={}",
                    event.getClass().getSimpleName());
                throw new RocketMqTransactionUnknownException(tag, event, null);

            default:
                throw new IllegalStateException("未知的事务状态：" + localTransactionState);
        }
    }


    /**
     * 将本地消息表状态转换为 RocketMQ 事务状态。
     *
     * @param status 本地消息表提交状态
     * @return RocketMQ 事务状态
     */
    static LocalTransactionState toLocalTransactionState(OutboxStore.CommitStatus status) {
        if (status == OutboxStore.CommitStatus.COMMITTED) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        if (status == OutboxStore.CommitStatus.ROLLED_BACK
            || status == OutboxStore.CommitStatus.NOT_FOUND) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
        return LocalTransactionState.UNKNOW;
    }


    /**
     * 将当前 MDC 中的 traceId 写入消息用户属性。
     *
     * @param msg RocketMQ 消息
     */
    static void attachTraceId(Message msg) {
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            msg.putUserProperty(TraceIdFilter.HEADER, traceId);
        }
    }

}
