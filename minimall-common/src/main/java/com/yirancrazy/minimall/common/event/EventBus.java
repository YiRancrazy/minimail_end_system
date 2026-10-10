package com.yirancrazy.minimall.common.event;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface EventBus {
    /**
     * 以普通消息发布事件。
     * @param event 要发布的领域事件
     */
    void publish(Object event);

    /**
     * 以本地事务发布事件。默认实现先执行本地事务再发布，因此不具备原子性。
     * RocketMQ 实现通过半消息语义覆盖该行为。
     * @param event 要发布的领域事件
     * @param localTx 提交消息前要执行的本地事务
     */
    default void publishInTx(Object event, Consumer<String> localTx) {
        localTx.accept(null);
        publish(event);
    }

    /**
     * 使用本地事务和 broker 回调 checker 发布事件。checker 用于探测本地事务是否已提交，
     * 从而使 broker 可以提交或回滚未确认的半消息。
     * @param event 要发布的领域事件
     * @param localTx 提交消息前要执行的本地事务
     * @param checker 本地事务已提交返回 true，回滚返回 false
     */
    default void publishInTx(Object event, Consumer<String> localTx, Function<Object, Boolean> checker) {
        publishInTx(event, localTx);
    }
}
