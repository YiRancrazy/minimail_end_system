package com.yirancrazy.minimall.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.yirancrazy.minimall.common.exception.mq.RocketMqTransactionRollbackException;
import com.yirancrazy.minimall.common.exception.mq.RocketMqTransactionUnknownException;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 Mock 事务生产者验证 RocketMQ 事件总线的消息路由、traceId 传播、
 * 事务状态映射、Outbox 回查和异常语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class RocketMqEventBusTest {

    /**
     * 测试目标：隔离用例之间的 MDC 状态。
     * 测试场景与前置条件：每个测试结束后调用。关键步骤：清空当前线程 MDC。
     * 预期结果：后续用例不会继承 traceId。
     */
    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /** * 验证traceId写属性；MDC值为tid-xyz；调用附加方法；预期消息属性等于该值. */
    @Test
    void testAttachTraceIdGivenMdcValueThenMessagePropertySet() {
        MDC.put(Result.TRACE_ID_KEY, "tid-xyz");
        Message msg = new Message("topic", "tag", new byte[0]);

        RocketMqEventBus.attachTraceId(msg);

        assertEquals("tid-xyz", msg.getUserProperty(TraceIdFilter.HEADER));
    }

    /** * 验证空traceId不写属性；MDC为空；调用附加方法；预期消息属性为null. */
    @Test
    void testAttachTraceIdGivenNoMdcValueThenNoPropertySet() {
        Message msg = new Message("topic", "tag", new byte[0]);

        RocketMqEventBus.attachTraceId(msg);

        assertNull(msg.getUserProperty(TraceIdFilter.HEADER));
    }

    /** * 验证发布携带traceId；注入生产者且MDC有值；执行普通发布；预期消息属性保留该值. */
    @Test
    void testPublishCarriesMdcTraceId() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        MDC.put(Result.TRACE_ID_KEY, "tid-abc");

        bus.publish("minimall-events", "DummyEvent", new DummyEvent("x"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(mockProducer).send(captor.capture());
        assertEquals("tid-abc", captor.getValue().getUserProperty(TraceIdFilter.HEADER));
    }

    /** * 验证明示路由；注入生产者；指定主题标签发布；预期消息主题标签一致. */
    @Test
    void testPublishWithRouteUsesProvidedTopicAndTag() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);

        bus.publish("outbox-topic", "outbox-tag", new DummyEvent("x"));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(mockProducer).send(captor.capture());
        assertEquals("outbox-topic", captor.getValue().getTopic());
        assertEquals("outbox-tag", captor.getValue().getTags());
    }

    /** * 验证事务ID传递；模拟事务发送tx-100；执行事务发布；预期回调收到tx-100. */
    @Test
    void testPublishInTxPassesTransactionIdToLocalTx() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.COMMIT_MESSAGE);
        final AtomicReference<String> captured = new AtomicReference<>();
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
                .thenAnswer(
                        inv -> {
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

    /** * 验证无生产者普通发布失败；未启动事件总线；调用普通发布；预期抛状态异常. */
    @Test
    void testPublishWithoutProducerThrows() {
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");

        assertThrows(
                IllegalStateException.class,
                () -> bus.publish("minimall-events", "DummyEvent", new DummyEvent("x")));
    }

    /** * 验证无生产者事务发布失败；未启动事件总线；调用事务发布；预期抛状态异常. */
    @Test
    void testPublishInTxWithoutProducerThrows() {
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");

        assertThrows(IllegalStateException.class, () -> bus.publishInTx(new DummyEvent("x"), tx -> {}));
    }

    /** * 验证缺少Outbox拒绝；注入生产者但未配置存储；调用事务发布；预期抛状态异常. */
    @Test
    void testPublishInTxWithoutOutboxStoreThrows() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);

        assertThrows(IllegalStateException.class, () -> bus.publishInTx(new DummyEvent("x"), tx -> {}));
    }

    /** * 验证事务发送失败；模拟broker不可用；调用事务发布；预期抛状态异常. */
    @Test
    void testPublishInTxSendFailureThrows() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
                .thenThrow(new RuntimeException("broker down"));
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        assertThrows(IllegalStateException.class, () -> bus.publishInTx(new DummyEvent("x"), tx -> {}));
    }

    /** * 验证回滚状态映射；事务结果为回滚；调用事务发布；预期抛事务回滚异常. */
    @Test
    void testPublishInTxRollbackStateThrowsRollback() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.ROLLBACK_MESSAGE);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
                .thenReturn(result);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        assertThrows(
                RocketMqTransactionRollbackException.class,
                () -> bus.publishInTx(new DummyEvent("x"), tx -> {}));
    }

    /** * 验证未知状态映射；事务结果为未知；调用事务发布；预期抛事务未知异常. */
    @Test
    void testPublishInTxUnknownStateThrowsUnknown() throws Exception {
        TransactionMQProducer mockProducer = Mockito.mock(TransactionMQProducer.class);
        TransactionSendResult result = new TransactionSendResult();
        result.setLocalTransactionState(LocalTransactionState.UNKNOW);
        Mockito.when(mockProducer.sendMessageInTransaction(Mockito.any(), Mockito.any()))
                .thenReturn(result);
        RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
        setProducer(bus, mockProducer);
        bus.setOutboxStore(Mockito.mock(OutboxStore.class));

        assertThrows(
                RocketMqTransactionUnknownException.class,
                () -> bus.publishInTx(new DummyEvent("x"), tx -> {}));
    }

    /** * 验证回查读取Outbox；模拟四种状态和读取异常；执行事务回查；预期映射一致. */
    @Test
    void testCheckLocalTransactionUsesOutboxStore() {
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
            final TransactionListener listener = listenerCaptor.getValue();

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

            assertEquals(
                    LocalTransactionState.COMMIT_MESSAGE,
                    listener.checkLocalTransaction(messageExt("tx-commit")));
            assertEquals(
                    LocalTransactionState.ROLLBACK_MESSAGE,
                    listener.checkLocalTransaction(messageExt("tx-rollback")));
            assertEquals(
                    LocalTransactionState.ROLLBACK_MESSAGE,
                    listener.checkLocalTransaction(messageExt("tx-missing")));
            assertEquals(
                    LocalTransactionState.UNKNOW, listener.checkLocalTransaction(messageExt("tx-unknown")));
            assertEquals(
                    LocalTransactionState.UNKNOW, listener.checkLocalTransaction(messageExt("tx-error")));
        }
    }

    /** * 验证无Outbox回查未知；启动总线但未配置存储；执行事务回查；预期返回未知. */
    @Test
    void testCheckLocalTransactionWithoutOutboxStoreReturnsUnknown() {
        try (MockedConstruction<TransactionMQProducer> mocked =
                Mockito.mockConstruction(TransactionMQProducer.class)) {
            RocketMqEventBus bus = new RocketMqEventBus("127.0.0.1:9876", "minimall-events");
            bus.start();
            TransactionMQProducer mockProducer = mocked.constructed().get(0);
            ArgumentCaptor<TransactionListener> listenerCaptor =
                    ArgumentCaptor.forClass(TransactionListener.class);
            Mockito.verify(mockProducer).setTransactionListener(listenerCaptor.capture());

            assertEquals(
                    LocalTransactionState.UNKNOW,
                    listenerCaptor.getValue().checkLocalTransaction(messageExt("tx-no-outbox")));
        }
    }

    /** * 验证默认未提交状态；匿名存储始终未提交；调用提交状态查询；预期返回未找到. */
    @Test
    void testDefaultOutboxStatusWithoutRecordIsNotFound() {
        OutboxStore store =
                /* 测试用匿名 OutboxStore：不记录提交，并始终报告本地事务未提交。 */
                new OutboxStore() {
                    /**
                     * 测试目标：满足接口契约，不记录真实提交状态。
                     *
                     * 测试场景与前置条件：默认状态查询用例不调用该方法。
                     * 关键步骤：空实现。预期结果：不产生副作用。
                     *
                     * @param messageKey 消息键
                     */
                    @Override
                    public void recordCommit(String messageKey) {}

                    /**
                     * 测试目标：模拟本地事务没有提交记录。
                     *
                     * 测试场景与前置条件：任意消息键均视为未提交。
                     * 关键步骤：返回 false。预期结果：默认 commitStatus 映射为 NOT_FOUND。
                     *
                     * @param messageKey 消息键
                     * @return 固定返回 false
                     */
                    @Override
                    public boolean isCommitted(String messageKey) {
                        return false;
                    }
                };

        assertEquals(OutboxStore.CommitStatus.NOT_FOUND, store.commitStatus("tx-missing"));
    }

    /** * 验证已提交映射；输入已提交状态；调用状态映射；预期返回提交消息. */
    @Test
    void testMapOutboxStatusCommittedThenCommit() {
        assertEquals(
                LocalTransactionState.COMMIT_MESSAGE,
                RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.COMMITTED));
    }

    /** * 验证已回滚映射；输入已回滚状态；调用状态映射；预期返回回滚消息. */
    @Test
    void testMapOutboxStatusRolledBackThenRollback() {
        assertEquals(
                LocalTransactionState.ROLLBACK_MESSAGE,
                RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.ROLLED_BACK));
    }

    /** * 验证未找到映射；输入未找到状态；调用状态映射；预期返回回滚消息. */
    @Test
    void testMapOutboxStatusNotFoundThenRollback() {
        assertEquals(
                LocalTransactionState.ROLLBACK_MESSAGE,
                RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.NOT_FOUND));
    }

    /** * 验证未知映射；输入未知状态；调用状态映射；预期返回未知. */
    @Test
    void testMapOutboxStatusUnknownThenUnknown() {
        assertEquals(
                LocalTransactionState.UNKNOW,
                RocketMqEventBus.toLocalTransactionState(OutboxStore.CommitStatus.UNKNOWN));
    }

    /**
     * 测试目标：通过反射向事件总线注入 Mock 生产者，避免启动真实 RocketMQ。
     *
     * @param bus 被测事件总线
     * @param producer Mock 事务生产者
     * @throws Exception 反射访问失败时抛出
     */
    private static void setProducer(RocketMqEventBus bus, TransactionMQProducer producer)
            throws Exception {
        Field field = RocketMqEventBus.class.getDeclaredField("producer");
        field.setAccessible(true);
        field.set(bus, producer);
    }

    /**
     * 创建带事务 ID、事件 tag 和序列化事件体的回查消息。
     *
     * @param transactionId 事务消息 ID
     * @return 可直接用于事务回查的 MessageExt
     */
    private static MessageExt messageExt(String transactionId) {
        MessageExt message = new MessageExt();
        message.setTags(MqEventJsonCodec.tagFor(DummyEvent.class));
        message.setTransactionId(transactionId);
        message.setBody(MqEventJsonCodec.encode(new DummyEvent("x")));
        return message;
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试用可序列化事件，携带一个字符串值并同时提供无参构造器。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     */
    public static class DummyEvent {
        private String value;

        /**
         * 创建携带字符串值的测试事件。
         *
         * @param value 事件值
         */
        DummyEvent(String value) {
            this.value = value;
        }

        /** 创建空测试事件，供 JSON 反序列化使用。 */
        public DummyEvent() {}

        /**
         * 返回事件中的字符串值。
         *
         * @return 字符串值
         */
        public String getValue() {
            return value;
        }

        /**
         * 设置事件中的字符串值。
         *
         * @param value 字符串值
         */
        public void setValue(String value) {
            this.value = value;
        }
    }
}
