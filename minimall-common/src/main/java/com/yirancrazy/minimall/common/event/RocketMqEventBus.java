package com.yirancrazy.minimall.common.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
public class RocketMqEventBus implements EventBus {

    private final String namesrvAddr;
    private final String topic;
    private TransactionMQProducer producer;
    /**
     * Optional persistent store for commit confirmation. When present, the
     * broker callback reads commit state from it; otherwise falls back to
     * the in-memory checker map which is lost on producer restart.
     */
    private OutboxStore outboxStore;
    private final Map<String, Function<Object, Boolean>> checkers = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> tagTypes = new ConcurrentHashMap<>();

    public RocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }

    /**
     * Inject a persistent {@link OutboxStore} for crash-safe callback.
     * @param outboxStore the store, or null to fall back to in-memory checkers
     */
    public void setOutboxStore(OutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    @PostConstruct
    void start() {
        producer = new TransactionMQProducer("minimall-producer");
        producer.setNamesrvAddr(namesrvAddr);
        producer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                Runnable localTx = (Runnable) arg;
                try {
                    localTx.run();
                    if (outboxStore != null && msg.getTransactionId() != null) {
                        outboxStore.recordCommit(msg.getTransactionId());
                    }
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                catch (Exception e) {
                    log.warn("local tx failed for {}: {}", msg.getTags(), e.getMessage());
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                if (outboxStore != null && msg.getTransactionId() != null) {
                    try {
                        return outboxStore.isCommitted(msg.getTransactionId())
                            ? LocalTransactionState.COMMIT_MESSAGE
                            : LocalTransactionState.ROLLBACK_MESSAGE;
                    }
                    catch (Exception e) {
                        log.warn("outbox check failed for tag={}: {}", msg.getTags(), e.getMessage());
                        return LocalTransactionState.UNKNOW;
                    }
                }
                Function<Object, Boolean> checker = checkers.get(msg.getTags());
                Class<?> type = tagTypes.get(msg.getTags());
                if (checker == null || type == null) {
                    log.warn("no checker for tag={}, returning UNKNOWN", msg.getTags());
                    return LocalTransactionState.UNKNOW;
                }
                try {
                    Object decoded = MqEventJsonCodec.decode(msg.getBody(), type);
                    return checker.apply(decoded) ? LocalTransactionState.COMMIT_MESSAGE
                        : LocalTransactionState.ROLLBACK_MESSAGE;
                }
                catch (Exception e) {
                    log.warn("checker failed for tag={}: {}", msg.getTags(), e.getMessage());
                    return LocalTransactionState.UNKNOW;
                }
            }
        });
        try {
            producer.start();
            log.info("rocketmq transaction producer started, namesrv={}, topic={}", namesrvAddr, topic);
        }
        catch (MQClientException e) {
            log.warn("rocketmq producer start failed, publish will be no-op: {}", e.getMessage());
            producer = null;
        }
    }

    @PreDestroy
    void stop() {
        if (producer != null) {
            producer.shutdown();
        }
    }

    /**
     * Deliver the given event to the configured RocketMQ topic using the
     * event's simple class name as the routing tag and its JSON encoding as
     * the message body. Producer outages are logged at WARN and swallowed so
     * the publish site is never blocked.
     *
     * @param event the domain event to send; encoded via {@link MqEventJsonCodec}
     */
    @Override
    public void publish(Object event) {
        if (producer == null) {
            log.warn("rocketmq producer not started; event {} dropped", event.getClass().getSimpleName());
            return;
        }
        try {
            Message msg = new Message(topic, MqEventJsonCodec.tagFor(event.getClass()),
                MqEventJsonCodec.encode(event));
            producer.send(msg);
            log.debug("rocketmq send ok for {}", event.getClass().getSimpleName());
        }
        catch (Exception e) {
            log.warn("rocketmq send failed for {}: {}", event.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * Publish an event as a transactional half-message. The local transaction
     * runs in the broker callback; on success the message is committed, on
     * exception it is rolled back.
     * @param event the domain event to publish
     * @param localTx the local transaction executed after the half-message is staged
     */
    @Override
    public void publishInTx(Object event, Runnable localTx) {
        publishInTx(event, localTx, e -> Boolean.TRUE);
    }

    /**
     * Publish an event as a transactional half-message with a checker for
     * broker callback. The checker is registered by event tag so the broker
     * can confirm commit/rollback when the producer fails to reply.
     * @param event the domain event to publish
     * @param localTx the local transaction executed after the half-message is staged
     * @param checker returns true to commit, false to roll back
     */
    @Override
    public void publishInTx(Object event, Runnable localTx, Function<Object, Boolean> checker) {
        if (producer == null) {
            log.warn("rocketmq producer not started; event {} dropped", event.getClass().getSimpleName());
            return;
        }
        String tag = MqEventJsonCodec.tagFor(event.getClass());
        checkers.put(tag, checker);
        tagTypes.put(tag, event.getClass());
        try {
            Message msg = new Message(topic, tag, MqEventJsonCodec.encode(event));
            TransactionSendResult r = producer.sendMessageInTransaction(msg, localTx);
            log.debug("rocketmq txn send ok for {}: state={}", event.getClass().getSimpleName(),
                r.getLocalTransactionState());
        }
        catch (Exception e) {
            log.warn("rocketmq txn send failed for {}: {}", event.getClass().getSimpleName(), e.getMessage());
        }
    }
}
