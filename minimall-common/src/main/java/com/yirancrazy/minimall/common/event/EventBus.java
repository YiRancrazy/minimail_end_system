package com.yirancrazy.minimall.common.event;

import java.util.function.Consumer;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线接口，定义事件的发布能力；支持普通发布与事务内发布两种方式。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface EventBus {

    /**
     * 发布事件, 立即将事件投递给所有订阅者，不考虑事务
     * @param event 待发布的事件对象
     */
    void publish(Object event);


    /**
     * 使用显式路由发布事件。
     * @param topic RocketMQ 主题，必须非空
     * @param tag RocketMQ 标签，必须非空
     * @param event 待发布的事件对象
     */
    void publish(String topic, String tag, Object event);


    /**
     * 在当前事务上下文中发布事件
     * @param event 待发布的事件对象
     * @param localTx 本地事务回调, 入参为事务标识
     */
    default void publishInTx(Object event, Consumer<String> localTx) {
        localTx.accept(null);
        publish(event);
    }
}
