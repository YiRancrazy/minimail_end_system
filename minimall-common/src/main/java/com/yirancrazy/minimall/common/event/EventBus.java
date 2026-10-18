package com.yirancrazy.minimall.common.event;

import java.util.function.Consumer;
import java.util.function.Function;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EventBus 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface EventBus {

    /**
     * 执行 publish 方法。
     * @param event event 参数
     */
    void publish(Object event);


    /**
     * 执行 publishInTx 方法。
     * @param event event 参数
     * @param localTx localTx 参数
     */
    default void publishInTx(Object event, Consumer<String> localTx) {
        localTx.accept(null);
        publish(event);
    }


    /**
     * 执行 publishInTx 方法。
     * @param event event 参数
     * @param localTx localTx 参数
     * @param checker checker 参数
     */
    default void publishInTx(Object event, Consumer<String> localTx, Function<Object, Boolean> checker) {
        publishInTx(event, localTx);
    }
}
