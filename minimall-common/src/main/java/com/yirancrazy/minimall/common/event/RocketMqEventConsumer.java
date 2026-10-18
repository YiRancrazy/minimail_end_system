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
 * @Description: RocketMqEventConsumer 类型，承载相关业务能力。
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
     * 构造 RocketMqEventConsumer 实例。
     * @param namesrvAddr namesrvAddr 参数
     * @param topic topic 参数
     * @param group group 参数
     */
    public RocketMqEventConsumer(String namesrvAddr, String topic, String group) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
        this.group = group;
    }


    /**
     * 执行 subscribe 方法。
     * @param <T> 泛型参数
     * @param eventType eventType 参数
     * @param handler handler 参数
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        handlers.put(eventType, e -> handler.accept(eventType.cast(e)));
        if (wrapper != null) {
            wrapper.registerHandler(eventType, handler);
        }
    }


    /**
     * 执行 start 方法。
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
     * 执行 stop 方法。
     */
    public void stop() {
        if (wrapper != null) {
            wrapper.shutdown();
        }
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: DefaultMQConsumerWrapper 类型，承载相关业务能力。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    static class DefaultMQConsumerWrapper {
        private final DefaultMQPushConsumer consumer;
        private final Map<Class<?>, Consumer<Object>> handlers;

        /**
         * 构造 DefaultMQConsumerWrapper 实例。
         * @param namesrvAddr namesrvAddr
         * @param topic topic
         * @param group group
         * @param handlers handlers
         * @throws Exception 处理失败时抛出
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
         * 创建Handler。
         * @param <T> 泛型参数
         * @param type 类型
         * @param handler handler
         */
        <T> void registerHandler(Class<T> type, Consumer<T> handler) {
            handlers.put(type, e -> handler.accept(type.cast(e)));
        }

        /**
         * 启动start。
         * @throws Exception 处理失败时抛出
         */
        void start() throws Exception {
            consumer.start();
        }

        /**
         * 处理shutdown。
         */
        void shutdown() {
            consumer.shutdown();
        }
    }
}
