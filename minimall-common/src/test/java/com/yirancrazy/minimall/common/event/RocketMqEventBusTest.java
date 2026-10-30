package com.yirancrazy.minimall.common.event;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.yirancrazy.minimall.common.exception.mq.RocketMqTransactionRollbackException;
import com.yirancrazy.minimall.common.exception.mq.RocketMqTransactionUnknownException;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 RocketMQ 事件总线的消息发送、事务状态和链路追踪行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
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
    void publishWithRoute_usesProvidedTopicAndTag() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);

        bus.publish("outbox-topic", "outbox-tag", new DummyEvent("x"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(mockProducer).send(captor.capture());
        assertEquals("outbox-topic", captor.getValue().getTopic());
        assertEquals("outbox-tag", captor.getValue().getTags());
    }

    @Test
    void publishInTx_passesTransactionIdToLocalTx() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.COMMIT_MESSAGE);
        AtomicReference<String> captured = new AtomicReference<>();
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
            .thenAnswer(inv -> {
                Message msg = inv.getArgument(0);
                msg.setTransactionId("tx-100");
                java.util.function.Consumer<String> tx = inv.getArgument(1);
                tx.accept("tx-100");
                return result;
            });
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        bus.publishInTx(new DummyEvent("x"), captured::set);

        assertEquals("tx-100", captured.get());
    }

    @Test
    void publish_withoutProducer_throws() {
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");

        assertThrows(IllegalStateException.class, () -> bus.publish(new DummyEvent("x")));
    }

    @Test
    void publishInTx_withoutProducer_throws() {
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");

        assertThrows(IllegalStateException.class,
            () -> bus.publishInTx(new DummyEvent("x"), tx -> { }));
    }

    @Test
    void publishInTx_withoutOutboxStore_throws() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);

        assertThrows(IllegalStateException.class,
            () -> bus.publishInTx(new DummyEvent("x"), tx -> { }));
    }

    @Test
    void publishInTx_sendFailure_throws() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
            .thenThrow(new RuntimeException("broker down"));
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        assertThrows(IllegalStateException.class,
            () -> bus.publishInTx(new DummyEvent("x"), tx -> { }));
    }

    @Test
    void publishInTx_rollbackState_throwsRollback() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.ROLLBACK_MESSAGE);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
            .thenReturn(result);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        assertThrows(RocketMqTransactionRollbackException.class,
            () -> bus.publishInTx(new DummyEvent("x"), tx -> { }));
    }

    @Test
    void publishInTx_unknownState_throwsUnknown() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.UNKNOW);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
            .thenReturn(result);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        assertThrows(RocketMqTransactionUnknownException.class,
            () -> bus.publishInTx(new DummyEvent("x"), tx -> { }));
    }

    @Test
    void checkLocalTransaction_usesOutboxStore() {
        try (MockedConstruction<TransactionMQProducer> mocked =
                 Mockito.mockConstruction(TransactionMQProducer.class)) {
            OutboxStore outboxStore = Mockito.mock(OutboxStore.class);
            RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
            bus.setOutboxStore(outboxStore);
            bus.start();
            TransactionMQProducer mockProducer = mocked.constructed().get(0);
            ArgumentCaptor<TransactionListener> listenerCaptor =
                ArgumentCaptor.forClass(TransactionListener.class);
            Mockito.verify(mockProducer).setTransactionListener(listenerCaptor.capture());
            TransactionListener listener = listenerCaptor.getValue();

            Mockito.when(outboxStore.commitStatus("tx-commit"))
                .thenReturn(OutboxStore.CommitStatus.COMMITTED);
            Mockito.when(outboxStore.commitStatus("tx-rollback"))
                .thenReturn(OutboxStore.CommitStatus.ROLLED_BACK);
            Mockito.when(outboxStore.commitStatus("tx-missing"))
                .thenReturn(OutboxStore.CommitStatus.NOT_FOUND);
            Mockito.when(outboxStore.commitStatus("tx-unknown"))
                .thenReturn(OutboxStore.CommitStatus.UNKNOWN);
            Mockito.when(outboxStore.commitStatus("tx-error"))
                .thenThrow(new IllegalStateException("db down"));

            assertEquals(LocalTransactionState.COMMIT_MESSAGE,
                listener.checkLocalTransaction(messageExt("tx-commit")));
            assertEquals(LocalTransactionState.ROLLBACK_MESSAGE,
                listener.checkLocalTransaction(messageExt("tx-rollback")));
            assertEquals(LocalTransactionState.ROLLBACK_MESSAGE,
                listener.checkLocalTransaction(messageExt("tx-missing")));
            assertEquals(LocalTransactionState.UNKNOW,
                listener.checkLocalTransaction(messageExt("tx-unknown")));
            assertEquals(LocalTransactionState.UNKNOW,
                listener.checkLocalTransaction(messageExt("tx-error")));
        }
    }

    @Test
    void checkLocalTransaction_withoutOutboxStore_returnsUnknown() {
        try (MockedConstruction<TransactionMQProducer> mocked =
                 Mockito.mockConstruction(TransactionMQProducer.class)) {
            RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
            bus.start();
            TransactionMQProducer mockProducer = mocked.constructed().get(0);
            ArgumentCaptor<TransactionListener> listenerCaptor =
                ArgumentCaptor.forClass(TransactionListener.class);
            Mockito.verify(mockProducer).setTransactionListener(listenerCaptor.capture());

            assertEquals(LocalTransactionState.UNKNOW,
                listenerCaptor.getValue().checkLocalTransaction(messageExt("tx-no-outbox")));
        }
    }

    @Test
    void defaultOutboxStatus_withoutRecord_isNotFound() {
        OutboxStore store = new OutboxStore() {
            @Override
            public void recordCommit(String messageKey) {
            }

            @Override
            public boolean isCommitted(String messageKey) {
                return false;
            }
        };

        assertEquals(OutboxStore.CommitStatus.NOT_FOUND, store.commitStatus("tx-missing"));
    }

    @Test
    void mapOutboxStatus_committed_thenCommit() {
        assertEquals(LocalTransactionState.COMMIT_MESSAGE,
            RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.COMMITTED));
    }

    @Test
    void mapOutboxStatus_rolledBack_thenRollback() {
        assertEquals(LocalTransactionState.ROLLBACK_MESSAGE,
            RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.ROLLED_BACK));
    }

    @Test
    void mapOutboxStatus_notFound_thenRollback() {
        assertEquals(LocalTransactionState.ROLLBACK_MESSAGE,
            RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.NOT_FOUND));
    }

    @Test
    void mapOutboxStatus_unknown_thenUnknown() {
        assertEquals(LocalTransactionState.UNKNOW,
            RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.UNKNOWN));
    }

    private static void setProducer(RocketMqEventBus bus, TransactionMQProducer producer) throws Exception {
        Field field = RocketMqEventBus.class.getDeclaredField("producer");
        field.setAccessible(true);
        field.set(bus, producer);
    }

    private static MessageExt messageExt(String transactionId) {
        MessageExt message = new MessageExt();
        message.setTags(MqEventJsonCodec.tagFor(DummyEvent.class));
        message.setTransactionId(transactionId);
        message.setBody(MqEventJsonCodec.encode(new DummyEvent("x")));
        return message;
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试事件，携带字符串值。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    public static class DummyEvent {
        private String value;

        DummyEvent(String value) {
            this.value = value;
        }

        /**
         * 创建空测试事件，供 JSON 反序列化。
         */
        public DummyEvent() {
        }

        /**
         * 获取事件值。
         *
         * @return 字符串值
         */
        public String getValue() {
            return value;
        }

        /**
         * 设置事件值。
         *
         * @param value 字符串值
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}
