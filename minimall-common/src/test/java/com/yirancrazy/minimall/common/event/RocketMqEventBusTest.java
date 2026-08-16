package com.yirancrazy.minimall.common.event;

import java.lang.reflect.Field;
import java.util.Map;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: RocketMqEventBus traceId 透传的单元测试类，验证生产者将 MDC traceId 写入消息属性。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 */
class RocketMqEventBusTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void attachTraceId_givenMdcValue_thenMessagePropertySet() {
        MDC.put(Result.TRACE_ID_KEY, "tid-xyz");
        Message msg = new Message("topic", "tag", new byte[0]);

        RocketMqEventBus.attachTraceId(msg);

        assertEquals("tid-xyz", msg.getUserProperty(TraceIdFilter.HEADER));
    }

    @Test
    void attachTraceId_givenNoMdcValue_thenNoPropertySet() {
        Message msg = new Message("topic", "tag", new byte[0]);

        RocketMqEventBus.attachTraceId(msg);

        assertNull(msg.getUserProperty(TraceIdFilter.HEADER));
    }

    @Test
    void publish_carriesMdcTraceId() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        MDC.put(Result.TRACE_ID_KEY, "tid-abc");

        bus.publish(new DummyEvent("x"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(mockProducer).send(captor.capture());
        assertEquals("tid-abc", captor.getValue().getUserProperty(TraceIdFilter.HEADER));
    }

    @Test
    void publishInTx_clearsCheckerRegistry() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.COMMIT_MESSAGE);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
            .thenReturn(result);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);

        bus.publishInTx(new DummyEvent("x"), () -> { }, e -> Boolean.TRUE);

        assertTrue(getFieldMap(bus, "checkers").isEmpty());
        assertTrue(getFieldMap(bus, "tagTypes").isEmpty());
    }

    private static void setProducer(RocketMqEventBus bus, TransactionMQProducer producer) throws Exception {
        Field field = RocketMqEventBus.class.getDeclaredField("producer");
        field.setAccessible(true);
        field.set(bus, producer);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> getFieldMap(RocketMqEventBus bus, String name) throws Exception {
        Field field = RocketMqEventBus.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<String, ?>) field.get(bus);
    }

    static class DummyEvent {
        private final String value;

        DummyEvent(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
