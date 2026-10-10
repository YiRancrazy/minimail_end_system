package com.yirancrazy.minimall.common.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
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
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
public class RocketMqEventBus implements EventBus {

    private final String namesrvAddr;
    private final String topic;
    private TransactionMQProducer producer;
    /**
     * 可选的持久化存储，用于提交确认。存在时，broker 回调从其中读取提交状态；
     * 否则回退到生产者重启后会丢失的内存 checker map。
     */
    private OutboxStore outboxStore;
    private final Map<String, Function<Object, Boolean>> checkers = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> tagTypes = new ConcurrentHashMap<>();

    public RocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }

    /**
     * 注入用于崩溃安全回调的持久化 {@link OutboxStore}。
     * @param outboxStore 存储实例；为 null 时回退到内存 checker
     */
    public void setOutboxStore(OutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    @PostConstruct
    void start() {
        producer = new TransactionMQProducer("minimall-producer");
        producer.setNamesrvAddr(namesrvAddr);
        producer.setTransactionListener(new TransactionListener() {
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

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                if (outboxStore != null && msg.getTransactionId() != null) {
                    try {
                        return toLocalTransactionState(
                            outboxStore.commitStatus(msg.getTransactionId()));
                    }
                    catch (Exception e) {
                        log.warn("outbox 状态检查失败，tag={}: {}", msg.getTags(), e.getMessage());
                        return LocalTransactionState.UNKNOW;
                    }
                }
                Function<Object, Boolean> checker = checkers.get(msg.getTags());
                Class<?> type = tagTypes.get(msg.getTags());
                if (checker == null || type == null) {
                    log.warn("tag={} 没有 checker，返回 UNKNOWN", msg.getTags());
                    return LocalTransactionState.UNKNOW;
                }
                try {
                    Object decoded = MqEventJsonCodec.decode(msg.getBody(), type);
                    return checker.apply(decoded) ? LocalTransactionState.COMMIT_MESSAGE
                        : LocalTransactionState.ROLLBACK_MESSAGE;
                }
                catch (Exception e) {
                    log.warn("checker 执行失败，tag={}: {}", msg.getTags(), e.getMessage());
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

    @PreDestroy
    void stop() {
        if (producer != null) {
            producer.shutdown();
        }
    }

    /**
     * 使用事件简单类名作为路由 Tag、JSON 编码作为消息体，将给定事件投递到配置的
     * RocketMQ topic。生产者不可用只记录 WARN 日志并吞掉异常，确保发布点不会被阻塞。
     *
     * @param event 要发送的领域事件；通过 {@link MqEventJsonCodec} 编码
     */
    @Override
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
     * 以事务半消息发布事件。本地事务在 broker 回调中运行；
     * 成功时提交消息，异常时回滚消息。
     * @param event 要发布的领域事件
     * @param localTx 半消息暂存后要执行的本地事务
     */
    @Override
    public void publishInTx(Object event, Consumer<String> localTx) {
        publishInTx(event, localTx, e -> Boolean.TRUE);
    }

    /**
     * 使用 broker 回调 checker 以事务半消息发布事件。checker 按事件 Tag 注册，
     * 使 broker 能在生产者未回复时确认提交或回滚。
     * @param event 要发布的领域事件
     * @param localTx 半消息暂存后要执行的本地事务
     * @param checker 提交返回 true，回滚返回 false
     */
    @Override
    public void publishInTx(Object event, Consumer<String> localTx, Function<Object, Boolean> checker) {
        if (producer == null) {
            throw new IllegalStateException("RocketMQ 生产者未启动");
        }
        String tag = MqEventJsonCodec.tagFor(event.getClass());
        checkers.put(tag, checker);
        tagTypes.put(tag, event.getClass());
        try {
            Message msg = new Message(topic, tag, MqEventJsonCodec.encode(event));
            attachTraceId(msg);
            TransactionSendResult r = producer.sendMessageInTransaction(msg, localTx);
            log.debug("rocketmq 事务消息发送成功，event={}，state={}", event.getClass().getSimpleName(),
                r.getLocalTransactionState());
        }
        catch (Exception e) {
            log.error("rocketmq 事务消息发送失败，event={}", event.getClass().getSimpleName(), e);
            throw new IllegalStateException("RocketMQ 事务消息发送失败", e);
        }
        finally {
            // checker 仅在发送窗口期供 broker 回查事务状态，发送流程结束即移除，避免注册表无界增长
            // （持久化回查应配置 OutboxStore；内存 checker 在进程重启后本就丢失）
            checkers.remove(tag);
            tagTypes.remove(tag);
        }
    }

    /**
     * 将 outbox 三态映射为 RocketMQ 本地事务状态。
     * @param status outbox 本地事务状态
     * @return RocketMQ 本地事务状态
     */
    static LocalTransactionState toLocalTransactionState(OutboxStore.CommitStatus status) {
        if (status == OutboxStore.CommitStatus.COMMITTED) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        if (status == OutboxStore.CommitStatus.ROLLED_BACK) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
        return LocalTransactionState.UNKNOW;
    }

    /**
     * 将当前链路 traceId 写入消息属性，供消费端沿用同一链路 ID；MDC 无值时不设置。
     * @param msg 待发送的消息
     */
    static void attachTraceId(Message msg) {
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            msg.putUserProperty(TraceIdFilter.HEADER, traceId);
        }
    }
}
