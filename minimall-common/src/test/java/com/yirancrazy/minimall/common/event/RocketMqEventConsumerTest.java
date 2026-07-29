package com.yirancrazy.minimall.common.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class RocketMqEventConsumerTest
{

    @Test
    void start_givenNoHandlers_thenLogsWarning(CapturedOutput output)
    {
        RocketMqEventConsumer consumer = new RocketMqEventConsumer("localhost:9876", "test-topic", "test-group");

        consumer.start();

        assertThat(output).contains(
            "RocketMqEventConsumer.start() called with no handlers registered; events will not be consumed");
    }

    @Test
    void consumeMessage_givenHandlerFailure_thenReconsumeLater() throws Exception
    {
        Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
        handlers.put(SampleEvent.class, event ->
        {
            throw new IllegalStateException("handler failed");
        });
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper = new RocketMqEventConsumer.DefaultMQConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

        ConsumeConcurrentlyStatus status = listenerFrom(wrapper).consumeMessage(
            List.of(message(MqEventJsonCodec.encode(new SampleEvent("payload")))), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);
    }

    @Test
    void consumeMessage_givenDecodeFailure_thenConsumesSuccessfully() throws Exception
    {
        Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
        handlers.put(SampleEvent.class, event ->
        {
        });
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper = new RocketMqEventConsumer.DefaultMQConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

        ConsumeConcurrentlyStatus status = listenerFrom(wrapper).consumeMessage(
            List.of(message(new byte[] {'{'})), null);

        assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
    }

    private static MessageListenerConcurrently listenerFrom(
        RocketMqEventConsumer.DefaultMQConsumerWrapper wrapper)
    {
        DefaultMQPushConsumer consumer = (DefaultMQPushConsumer) ReflectionTestUtils.getField(wrapper, "consumer");
        assertThat(consumer).isNotNull();
        return (MessageListenerConcurrently) consumer.getMessageListener();
    }

    private static MessageExt message(byte[] body)
    {
        MessageExt message = new MessageExt();
        message.setTags(MqEventJsonCodec.tagFor(SampleEvent.class));
        message.setBody(body);
        return message;
    }

    private record SampleEvent(String payload)
    {
    }
}
