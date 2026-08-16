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
     * Register a handler to be invoked whenever a message with the tag
     * corresponding to {@code eventType} is received. The handler is stored
     * eagerly so it can be replayed on {@link #start()}; if the consumer has
     * already started, the handler is also attached to the live wrapper.
     *
     * @param eventType the event {@link Class} whose tag this handler should react to
     * @param handler the typed {@link Consumer} invoked with the decoded payload
     * @param <T> the event type handled by {@code handler}
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.put(eventType, e -> handler.accept(eventType.cast(e)));
        if (wrapper != null) {
            wrapper.registerHandler(eventType, handler);
        }
    }

    /**
     * Bring the underlying push consumer online and wire all previously
     * registered handlers. When no handlers were registered this is a
     * logged no-op; on startup failure the wrapper is dropped and events are
     * silently ignored.
     */
    public void start() {
        if (handlers.isEmpty()) {
            log.warn("RocketMqEventConsumer.start() called with no handlers registered; events will not be consumed");
            return;
        }
        try {
            wrapper = new DefaultMQConsumerWrapper(namesrvAddr, topic, group, handlers);
            wrapper.start();
            log.info("rocketmq consumer started, group={}, topic={}", group, topic);
        }
        catch (Exception e) {
            log.warn("rocketmq consumer start failed (events ignored): {}", e.getMessage());
            wrapper = null;
        }
    }

    /**
     * Shut down the underlying push consumer if it was started, releasing
     * the RocketMQ client resources. Safe to call when the consumer never
     * successfully started.
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
                            log.warn("rocketmq consumer decode failed for tag={}: {}", tag, ex.getMessage());
                            continue;
                        }
                        try {
                            handlers.get(matched).accept(payload);
                        }
                        catch (Exception ex) {
                            log.warn("rocketmq handler failed for tag={}: {}", tag, ex.getMessage());
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
