package com.yirancrazy.minimall.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: LocalEventBus description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class LocalEventBus implements ApplicationEventPublisherAware, EventBus {

    private ApplicationEventPublisher publisher;

    /**
     * Spring {@link ApplicationEventPublisherAware} callback that injects the
     * application-level event publisher used by {@link #publish(Object)}.
     *
     * @param publisher the Spring-managed {@link ApplicationEventPublisher} to delegate to; must not be {@code null}
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Object event) {
        if (publisher == null) {
            throw new IllegalStateException("LocalEventBus not initialized");
        }
        publisher.publishEvent(event);
    }
}