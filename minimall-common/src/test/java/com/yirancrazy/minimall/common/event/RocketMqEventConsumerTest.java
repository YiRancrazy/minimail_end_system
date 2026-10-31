package com.yirancrazy.minimall.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 RocketMQ 消费者的启动保护、消息分发失败重试、解码失败重试和 MDC 生命周期。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(OutputCaptureExtension.class)
class RocketMqEventConsumerTest {

  /** * 验证无处理器不启动；消费者未订阅；调用启动；预期输出未注册告警. */
  @Test
  void testStartGivenNoHandlersThenLogsWarning(CapturedOutput output) {
    RocketMqEventConsumer consumer =
        new RocketMqEventConsumer("localhost:9876", "test-topic", "test-group");

    consumer.start();

    assertThat(output).contains("RocketMqEventConsumer.start() 调用时未注册 handler，事件将不会被消费");
  }

  /** * 验证处理失败重投；处理器主动抛错；监听器消费消息；预期返回稍后重试. */
  @Test
  void testConsumeMessageGivenHandlerFailureThenReconsumeLater() throws Exception {
    Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
    handlers.put(
        SampleEvent.class,
        event -> {
          throw new IllegalStateException("handler failed");
        });
    RocketMqEventConsumer.DefaultMqConsumerWrapper wrapper =
        new RocketMqEventConsumer.DefaultMqConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

    ConsumeConcurrentlyStatus status =
        listenerFrom(wrapper)
            .consumeMessage(
                List.of(message(MqEventJsonCodec.encode(new SampleEvent("payload")))), null);

    assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);
  }

  /** * 验证解码失败重投；消息体为非法JSON；监听器消费消息；预期返回稍后重试. */
  @Test
  void testConsumeMessageGivenDecodeFailureThenReconsumeLater() throws Exception {
    Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
    handlers.put(SampleEvent.class, event -> {});
    RocketMqEventConsumer.DefaultMqConsumerWrapper wrapper =
        new RocketMqEventConsumer.DefaultMqConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

    ConsumeConcurrentlyStatus status =
        listenerFrom(wrapper).consumeMessage(List.of(message(new byte[] {'{'})), null);

    assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.RECONSUME_LATER);
  }

  /** * 验证traceId注入MDC；消息属性含traceId；监听器消费消息；预期处理时可见且结束后清理. */
  @Test
  void testConsumeMessageGivenTraceIdPropertyThenMdcPopulatedAndCleared() throws Exception {
    Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
    AtomicReference<String> seen = new AtomicReference<>();
    handlers.put(SampleEvent.class, event -> seen.set(MDC.get(Result.TRACE_ID_KEY)));
    RocketMqEventConsumer.DefaultMqConsumerWrapper wrapper =
        new RocketMqEventConsumer.DefaultMqConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);
    MessageExt msg = message(MqEventJsonCodec.encode(new SampleEvent("payload")));
    msg.putUserProperty(TraceIdFilter.HEADER, "tid-mq-001");

    ConsumeConcurrentlyStatus status = listenerFrom(wrapper).consumeMessage(List.of(msg), null);

    assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
    assertThat(seen).hasValue("tid-mq-001");
    assertThat(MDC.get(Result.TRACE_ID_KEY)).isNull();
  }

  /** * 验证缺失traceId生成；消息属性无traceId；监听器消费消息；预期处理后生成值且清理. */
  @Test
  void testConsumeMessageGivenNoTraceIdPropertyThenGeneratedAndCleared() throws Exception {
    Map<Class<?>, Consumer<Object>> handlers = new ConcurrentHashMap<>();
    AtomicReference<String> seen = new AtomicReference<>();
    handlers.put(SampleEvent.class, event -> seen.set(MDC.get(Result.TRACE_ID_KEY)));
    RocketMqEventConsumer.DefaultMqConsumerWrapper wrapper =
        new RocketMqEventConsumer.DefaultMqConsumerWrapper(
            "localhost:9876", "test-topic", "test-group", handlers);

    ConsumeConcurrentlyStatus status =
        listenerFrom(wrapper)
            .consumeMessage(
                List.of(message(MqEventJsonCodec.encode(new SampleEvent("payload")))), null);

    assertThat(status).isEqualTo(ConsumeConcurrentlyStatus.CONSUME_SUCCESS);
    assertThat(seen).isNotNull();
    assertThat(MDC.get(Result.TRACE_ID_KEY)).isNull();
  }

  /**
   * 获取包装器内部的实际消息监听器。
   *
   * @param wrapper 已创建底层消费者的包装器
   * @return 注册到 DefaultMQPushConsumer 的并发消息监听器
   */
  private static MessageListenerConcurrently listenerFrom(
      RocketMqEventConsumer.DefaultMqConsumerWrapper wrapper) {
    DefaultMQPushConsumer consumer =
        (DefaultMQPushConsumer) ReflectionTestUtils.getField(wrapper, "consumer");
    assertThat(consumer).isNotNull();
    return (MessageListenerConcurrently) consumer.getMessageListener();
  }

  /**
   * 创建带 SampleEvent tag 和指定消息体的 RocketMQ 消息。
   *
   * @param body 消息体字节数组
   * @return 测试用 MessageExt
   */
  private static MessageExt message(byte[] body) {
    MessageExt message = new MessageExt();
    message.setTags(MqEventJsonCodec.tagFor(SampleEvent.class));
    message.setBody(body);
    return message;
  }

  /**
   * @Author: yirancrazy@gmail.com
   * @Description: 测试用不可变事件，携带单个字符串载荷。
   * @Version: 1.0
   * @DateTime: 2026/10/16
   */
  private record SampleEvent(String payload) {}
}
