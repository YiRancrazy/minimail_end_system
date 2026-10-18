package com.yirancrazy.minimall.common.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: RocketMqEventBus 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
public class RocketMqEventBus implements EventBus {

    private final String namesrvAddr;
    private final String topic;
    private TransactionMQProducer producer;

    private OutboxStore outboxStore;
    private final Map<String, Function<Object, Boolean>> checkers = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> tagTypes = new ConcurrentHashMap<>();

    /**
     * 构造 RocketMqEventBus 实例。
     * @param namesrvAddr namesrvAddr 参数
     * @param topic topic 参数
     */
    public RocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }


    /**
     * 设置相关数据。
     * @param outboxStore outboxStore 参数
     */
    public void setOutboxStore(OutboxStore outboxStore) {
        this.outboxStore = outboxStore;
    }

    /**
     * 启动start。
     */
    @PostConstruct
    void start() {
        producer = new TransactionMQProducer("minimall-producer");
        producer.setNamesrvAddr(namesrvAddr);
        producer.setTransactionListener(new TransactionListener() {
            /**
             * 执行 executeLocalTransaction 方法。
             * @param msg msg 参数
             * @param arg arg 参数
             * @return 处理结果
             */
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                @SuppressWarnings("unchecked")
                Consumer<String> localTx = (Consumer<String>) arg;
                try {
                    localTx.accept(msg.getTransactionId());
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                catch (Exception e) {
                    log.warn("本地事务执行失败，tag={}: {}", msg.getTags(), e.getMessage());
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
            }

            /**
             * 校验业务条件。
             * @param msg msg 参数
             * @return 处理结果
             */
            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                if (outboxStore != null && msg.getTransactionId() != null) {
                    try {
                        return toLocalTransactionState(
                            outboxStore.commitStatus(msg.getTransactionId()));
                    }
                    catch (Exception e) {
                        log.warn("outbox 状态检查失败，tag={}: {}", msg.getTags(), e.getMessage());
                        return LocalTransactionState.UNKNOW;
                    }
                }
                Function<Object, Boolean> checker = checkers.get(msg.getTags());
                Class<?> type = tagTypes.get(msg.getTags());
                if (checker == null || type == null) {
                    log.warn("tag={} 没有 checker，返回 UNKNOWN", msg.getTags());
                    return LocalTransactionState.UNKNOW;
                }
                try {
                    Object decoded = MqEventJsonCodec.decode(msg.getBody(), type);
                    return checker.apply(decoded) ? LocalTransactionState.COMMIT_MESSAGE
                        : LocalTransactionState.ROLLBACK_MESSAGE;
                }
                catch (Exception e) {
                    log.warn("checker 执行失败，tag={}: {}", msg.getTags(), e.getMessage());
                    return LocalTransactionState.UNKNOW;
                }
            }
        });
        try {
            producer.start();
            log.info("rocketmq 事务生产者已启动，namesrv={}，topic={}", namesrvAddr, topic);
        }
        catch (MQClientException e) {
            log.error("rocketmq 生产者启动失败", e);
            producer = null;
            throw new IllegalStateException("RocketMQ 生产者启动失败", e);
        }
    }

    /**
     * 停止stop。
     */
    @PreDestroy
    void stop() {
        if (producer != null) {
            producer.shutdown();
        }
    }


    /**
     * 执行 publish 方法。
     * @param event event 参数
     */
    public void publish(Object event) {
        if (producer == null) {
            throw new IllegalStateException("RocketMQ 生产者未启动");
        }
        try {
            Message msg = new Message(topic, MqEventJsonCodec.tagFor(event.getClass()),
                MqEventJsonCodec.encode(event));
            attachTraceId(msg);
            producer.send(msg);
            log.debug("rocketmq 发送成功，event={}", event.getClass().getSimpleName());
        }
        catch (Exception e) {
            log.error("rocketmq 发送失败，event={}", event.getClass().getSimpleName(), e);
            throw new IllegalStateException("RocketMQ 消息发送失败", e);
        }
    }


    /**
     * 执行 publishInTx 方法。
     * @param event event 参数
     * @param localTx localTx 参数
     */
    public void publishInTx(Object event, Consumer<String> localTx) {
        publishInTx(event, localTx, e -> Boolean.TRUE);
    }


    /**
     * 执行 publishInTx 方法。
     * @param event event 参数
     * @param localTx localTx 参数
     * @param checker checker 参数
     */
    public void publishInTx(Object event, Consumer<String> localTx, Function<Object, Boolean> checker) {
        if (producer == null) {
            throw new IllegalStateException("RocketMQ 生产者未启动");
        }
        String tag = MqEventJsonCodec.tagFor(event.getClass());
        checkers.put(tag, checker);
        tagTypes.put(tag, event.getClass());
        try {
            Message msg = new Message(topic, tag, MqEventJsonCodec.encode(event));
            attachTraceId(msg);
            TransactionSendResult r = producer.sendMessageInTransaction(msg, localTx);
            log.debug("rocketmq 事务消息发送成功，event={}，state={}", event.getClass().getSimpleName(),
                r.getLocalTransactionState());
        }
        catch (Exception e) {
            log.error("rocketmq 事务消息发送失败，event={}", event.getClass().getSimpleName(), e);
            throw new IllegalStateException("RocketMQ 事务消息发送失败", e);
        }
        finally {
            // checker 仅在发送窗口期供 broker 回查事务状态，发送流程结束即移除，避免注册表无界增长
            // （持久化回查应配置 OutboxStore；内存 checker 在进程重启后本就丢失）
            checkers.remove(tag);
            tagTypes.remove(tag);
        }
    }


    /**
     * 转换或解析本地事务状态。
     * @param status 状态
     * @return 处理结果
     */
    static LocalTransactionState toLocalTransactionState(OutboxStore.CommitStatus status) {
        if (status == OutboxStore.CommitStatus.COMMITTED) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
        if (status == OutboxStore.CommitStatus.ROLLED_BACK) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
        return LocalTransactionState.UNKNOW;
    }


    /**
     * 附加链路追踪ID。
     * @param msg 提示消息
     */
    static void attachTraceId(Message msg) {
        String traceId = MDC.get(Result.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            msg.putUserProperty(TraceIdFilter.HEADER, traceId);
        }
    }
}
