package com.yirancrazy.minimall.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线组件，提供事件发布订阅能力
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class LocalEventBus implements ApplicationEventPublisherAware, EventBus {

    private ApplicationEventPublisher publisher;

    /**
     * Spring {@link ApplicationEventPublisherAware} 回调，注入
     * {@link #publish(Object)} 使用的应用级事件发布器。
     *
     * @param publisher Spring 管理的 {@link ApplicationEventPublisher}，用于委托发布；不能为 {@code null}
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Object event) {
        if (publisher == null) {
            throw new IllegalStateException("LocalEventBus 未初始化");
        }
        publisher.publishEvent(event);
    }
}
