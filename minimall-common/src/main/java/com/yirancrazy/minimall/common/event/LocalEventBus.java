package com.yirancrazy.minimall.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: LocalEventBus 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class LocalEventBus implements ApplicationEventPublisherAware, EventBus {

    private ApplicationEventPublisher publisher;


    /**
     * 设置相关数据。
     * @param publisher publisher 参数
     */
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /**
     * 执行 publish 方法。
     * @param event event 参数
     */
    public void publish(Object event) {
        if (publisher == null) {
            throw new IllegalStateException("LocalEventBus 未初始化");
        }
        publisher.publishEvent(event);
    }
}
