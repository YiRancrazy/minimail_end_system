package com.yirancrazy.minimall.common.event;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.MDC;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RocketMqEvent消费者，消费RocketMqEvent相关消息
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
public class RocketMqEventConsumer {

    private final String namesrvAddr;
    private final String topic;
    private final String group;
    private DefaultMQConsumerWrapper wrapper;
    private final Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();

    public RocketMqEventConsumer(String namesrvAddr, String topic, String group) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
        this.group = group;
    }

    /**
     * 注册 handler，当收到与 {@code eventType} 对应 Tag 的消息时调用。
     * handler 会被提前存储，以便 {@link #start()} 时重放；如果 consumer
     * 已启动，handler 也会挂到实时 wrapper 上。
     *
     * @param eventType 当前 handler 要响应的事件 {@link Class} Tag
     * @param handler 处理解码后载荷的强类型 {@link Consumer}
     * @param <T> {@code handler} 处理的事件类型
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.put(eventType, e -> handler.accept(eventType.cast(e)));
        if (wrapper != null) {
            wrapper.registerHandler(eventType, handler);
        }
    }

    /**
     * 启动底层 push consumer，并连接所有此前注册的 handler。
     * 未注册 handler 时只记录日志并直接返回；启动失败时丢弃 wrapper，
     * 事件被静默忽略。
     */
    public void start() {
        if (handlers.isEmpty()) {
            log.warn("RocketMqEventConsumer.start() 调用时未注册 handler，事件将不会被消费");
            return;
        }
        try {
            wrapper = new DefaultMQConsumerWrapper(namesrvAddr, topic, group, handlers);
            wrapper.start();
            log.info("rocketmq 消费者已启动，group={}，topic={}", group, topic);
        }
        catch (Exception e) {
            log.error("rocketmq 消费者启动失败", e);
            wrapper = null;
            throw new IllegalStateException("RocketMQ 消费者启动失败", e);
        }
    }

    /**
     * 如果底层 push consumer 已启动则关闭，释放 RocketMQ 客户端资源。
     * consumer 从未成功启动时调用也安全。
     */
    public void stop() {
        if (wrapper != null) {
            wrapper.shutdown();
        }
    }

    static class DefaultMQConsumerWrapper {
        private final DefaultMQPushConsumer consumer;
        private final Map<Class<?>, Consumer<Object>> handlers;

        DefaultMQConsumerWrapper(String namesrvAddr, String topic, String group,
                                 Map<Class<?>, Consumer<Object>> handlers) throws Exception {
            this.handlers = handlers;
            this.consumer = new DefaultMQPushConsumer(group);
            this.consumer.setNamesrvAddr(namesrvAddr);
            this.consumer.subscribe(topic, "*");
            this.consumer.registerMessageListener((MessageListenerConcurrently) (msgs, ctx) -> {
                for (var msg : msgs) {
                    // 从消息属性还原生产者线程的 traceId 写入 MDC，处理结束清理，使消费日志归属同一链路
                    String traceId = msg.getUserProperty(TraceIdFilter.HEADER);
                    MDC.put(Result.TRACE_ID_KEY, traceId == null || traceId.isBlank()
                        ? UUID.randomUUID().toString().replace("-", "")
                        : traceId);
                    try {
                        String tag = msg.getTags();
                        Class<?> matched = null;
                        for (var e : handlers.entrySet()) {
                            if (MqEventJsonCodec.tagFor(e.getKey()).equals(tag)) {
                                matched = e.getKey();
                                break;
                            }
                        }
                        if (matched == null) {
                            continue;
                        }
                        Object payload;
                        try {
                            payload = MqEventJsonCodec.decode(msg.getBody(), matched);
                        }
                        catch (Exception ex) {
                            // 解码失败不静默丢弃：返回 RECONSUME_LATER 由 RocketMQ 重投，重试耗尽进死信队列
                            log.warn("rocketmq 消费者解码失败，tag={}，稍后重试", tag, ex);
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                        try {
                            handlers.get(matched).accept(payload);
                        }
                        catch (Exception ex) {
                            log.warn("rocketmq handler 执行失败，tag={}: {}", tag, ex.getMessage());
                            return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                        }
                    }
                    finally {
                        MDC.remove(Result.TRACE_ID_KEY);
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
        }

        <T> void registerHandler(Class<T> type, Consumer<T> handler) {
            handlers.put(type, e -> handler.accept(type.cast(e)));
        }

        void start() throws Exception {
            consumer.start();
        }

        void shutdown() {
            consumer.shutdown();
        }
    }
}
