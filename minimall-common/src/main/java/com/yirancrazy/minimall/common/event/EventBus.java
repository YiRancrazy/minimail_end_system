package com.yirancrazy.minimall.common.event;

import java.util.function.Function;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface EventBus {
    /**
     * Publish an event as a plain message.
     * @param event the domain event to publish
     */
    void publish(Object event);

    /**
     * Publish an event with a local transaction. Default implementation runs
     * the local transaction then publishes, which is NOT atomic. RocketMQ
     * implementations override this with half-message semantics.
     * @param event the domain event to publish
     * @param localTx the local transaction to execute before committing the message
     */
    default void publishInTx(Object event, Runnable localTx) {
        localTx.run();
        publish(event);
    }

    /**
     * Publish an event with a local transaction and a checker for broker
     * callback. The checker probes whether the local transaction committed,
     * so the broker can commit or roll back an unconfirmed half-message.
     * @param event the domain event to publish
     * @param localTx the local transaction to execute before committing the message
     * @param checker returns true if the local transaction committed, false to roll back
     */
    default void publishInTx(Object event, Runnable localTx, Function<Object, Boolean> checker) {
        publishInTx(event, localTx);
    }
}
