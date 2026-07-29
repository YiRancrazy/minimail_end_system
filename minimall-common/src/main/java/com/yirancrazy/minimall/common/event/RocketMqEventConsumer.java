package com.yirancrazy.minimall.common.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Per-event-type consumer helper. Service code calls
 * `consumer.subscribe(OrderPaidDTO.class, this::handle)`. The consumer must
 * be started by the service (e.g., in @PostConstruct) and is no-op when
 * RocketMQ is not enabled.
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

    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.put(eventType, e -> handler.accept(eventType.cast(e)));
        if (wrapper != null) {
            wrapper.registerHandler(eventType, handler);
        }
    }

    public void start() {
        if (handlers.isEmpty()) {
            log.warn("RocketMqEventConsumer.start() called with no handlers registered; events will not be consumed");
            return;
        }
        try {
            wrapper = new DefaultMQConsumerWrapper(namesrvAddr, topic, group, handlers);
            wrapper.start();
            log.info("rocketmq consumer started, group={}, topic={}", group, topic);
        } catch (Exception e) {
            log.warn("rocketmq consumer start failed (events ignored): {}", e.getMessage());
            wrapper = null;
        }
    }

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
                    String tag = msg.getTags();
                    Class<?> matched = null;
                    for (var e : handlers.entrySet()) {
                        if (MqEventJsonCodec.tagFor(e.getKey()).equals(tag)) {
                            matched = e.getKey();
                            break;
                        }
                    }
                    if (matched == null) continue;
                    Object payload;
                    try {
                        payload = MqEventJsonCodec.decode(msg.getBody(), matched);
                    } catch (Exception ex) {
                        log.warn("rocketmq consumer decode failed for tag={}: {}", tag, ex.getMessage());
                        continue;
                    }
                    try {
                        handlers.get(matched).accept(payload);
                    } catch (Exception ex) {
                        log.warn("rocketmq handler failed for tag={}: {}", tag, ex.getMessage());
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
        }

        <T> void registerHandler(Class<T> type, Consumer<T> handler) {
            handlers.put(type, e -> handler.accept(type.cast(e)));
        }

        void start() throws Exception { consumer.start(); }
        void shutdown() { consumer.shutdown(); }
    }
}
