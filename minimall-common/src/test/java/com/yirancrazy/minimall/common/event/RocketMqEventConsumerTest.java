package com.yirancrazy.minimall.common.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RocketMqEventConsumer 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@ExtendWith(OutputCaptureExtension.class)
class RocketMqEventConsumerTest {

    @Test
    void start_givenNoHandlers_thenLogsWarning(CapturedOutput output) {
        RocketMqEventConsumer consumer = new RocketMqEventConsumer("localhost:9876", "test-topic", "test-group");

        consumer.start();

        assertThat(output).contains(
            "RocketMqEventConsumer.start() called with no handlers registered; events will not be consumed");
    }

    @Test
    void consumeMessage_givenHandlerFailure_thenReconsumeLater() throws Exception {
        Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
        handlers.put(SampleEvent.class, event -> {
            throw new IllegalStateException("handler failed");
        });
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper = new RocketMqEventConsumer.DefaultMQConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

        ConsumeConcurrentlyStatus status = listenerFrom(wrapper).consumeMessage(
            List.of(message(MqEventJsonCodec.encode(new SampleEvent("payload")))), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);
    }

    @Test
    void consumeMessage_givenDecodeFailure_thenConsumesSuccessfully() throws Exception {
        Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
        handlers.put(SampleEvent.class, event -> {
        });
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper = new RocketMqEventConsumer.DefaultMQConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

        ConsumeConcurrentlyStatus status = listenerFrom(wrapper).consumeMessage(
            List.of(message(new byte[] {'{'})), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
    }

    @Test
    void consumeMessage_givenTraceIdProperty_thenMdcPopulatedAndCleared() throws Exception {
        Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
        AtomicReference<String> seen = new AtomicReference<>();
        handlers.put(SampleEvent.class, event -> seen.set(MDC.get(Result.TRACE_ID_KEY)));
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper = new RocketMqEventConsumer.DefaultMQConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);
        MessageExt msg = message(MqEventJsonCodec.encode(new SampleEvent("payload")));
        msg.putUserProperty(TraceIdFilter.HEADER, "tid-mq-001");

        ConsumeConcurrentlyStatus status = listenerFrom(wrapper).consumeMessage(List.of(msg), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
        assertThat(seen).hasValue("tid-mq-001");
        assertThat(MDC.get(Result.TRACE_ID_KEY)).isNull();
    }

    @Test
    void consumeMessage_givenNoTraceIdProperty_thenGeneratedAndCleared() throws Exception {
        Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
        AtomicReference<String> seen = new AtomicReference<>();
        handlers.put(SampleEvent.class, event -> seen.set(MDC.get(Result.TRACE_ID_KEY)));
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper = new RocketMqEventConsumer.DefaultMQConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

        ConsumeConcurrentlyStatus status = listenerFrom(wrapper)
            .consumeMessage(List.of(message(MqEventJsonCodec.encode(new SampleEvent("payload")))), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
        assertThat(seen).isNotNull();
        assertThat(MDC.get(Result.TRACE_ID_KEY)).isNull();
    }

    private static MessageListenerConcurrently listenerFrom(
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper) {
        DefaultMQPushConsumer consumer = (DefaultMQPushConsumer) ReflectionTestUtils.getField(wrapper, "consumer");
        assertThat(consumer).isNotNull();
        return (MessageListenerConcurrently) consumer.getMessageListener();
    }

    private static MessageExt message(byte[] body) {
        MessageExt message = new MessageExt();
        message.setTags(MqEventJsonCodec.tagFor(SampleEvent.class));
        message.setBody(body);
        return message;
    }

    private record SampleEvent(String payload) {
    }
}
