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
 * @Description: RocketMQ 事件消费者，按事件类型注册处理器并将消息分发到对应处理器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
public class RocketMqEventConsumer {

    private final String namesrvAddr;
    private final String topic;
    private final String group;
    private DefaultMQConsumerWrapper wrapper;
    private final Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();

    /**
     * 创建 RocketMQ 事件消费者。
     *
     * @param namesrvAddr RocketMQ NameServer 地址
     * @param topic 事件消息主题
     * @param group 消费者组
     */
    public RocketMqEventConsumer(String namesrvAddr, String topic, String group) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
        this.group = group;
    }


    /**
     * 订阅指定类型的事件。
     *
     * @param <T> 事件类型
     * @param eventType 事件类型
     * @param handler 事件处理器
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.put(eventType, e -> handler.accept(eventType.cast(e)));
        if (wrapper != null) {
            wrapper.registerHandler(eventType, handler);
        }
    }


    /**
     * 启动 RocketMQ 消费者。
     * 未注册任何处理器时不启动消费者。
     *
     * @throws IllegalStateException 消费者启动失败时抛出
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
     * 关闭 RocketMQ 消费者。
     */
    public void stop() {
        if (wrapper != null) {
            wrapper.shutdown();
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: RocketMQ 默认推送消费者包装器，负责订阅、消息分发和消费状态返回。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    static class DefaultMQConsumerWrapper {
        private final DefaultMQPushConsumer consumer;
        private final Map<Class<?>, Consumer<Object>> handlers;

        /**
         * 创建默认推送消费者包装器。
         *
         * @param namesrvAddr RocketMQ NameServer 地址
         * @param topic 事件消息主题
         * @param group 消费者组
         * @param handlers 事件类型与处理器的映射
         * @throws Exception 消费者初始化失败时抛出
         */
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

        /**
         * 注册或替换指定类型的事件处理器。
         *
         * @param <T> 事件类型
         * @param type 事件类型
         * @param handler 事件处理器
         */
        <T> void registerHandler(Class<T> type, Consumer<T> handler) {
            handlers.put(type, e -> handler.accept(type.cast(e)));
        }

        /**
         * 启动底层推送消费者。
         *
         * @throws Exception 消费者启动失败时抛出
         */
        void start() throws Exception {
            consumer.start();
        }

        /**
         * 关闭底层推送消费者。
         */
        void shutdown() {
            consumer.shutdown();
        }
    }
}
