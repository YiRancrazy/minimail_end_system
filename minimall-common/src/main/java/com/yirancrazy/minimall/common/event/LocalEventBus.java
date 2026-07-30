package com.yirancrazy.minimall.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Iter-2 进程内事件总线。基于 Spring {@link ApplicationEventPublisher}，避免引入
 * RocketMQ。Subscriber 通过 {@code @EventListener} 自动注册。
 * <p>
 * Iter-3 替换为 RocketMQ 实现时，本类签名保持不变。
 */
@Component
public class LocalEventBus implements ApplicationEventPublisherAware, EventBus
{

    private ApplicationEventPublisher publisher;

    /**
     * Spring {@link ApplicationEventPublisherAware} callback that injects the
     * application-level event publisher used by {@link #publish(Object)}.
     *
     * @param publisher the Spring-managed {@link ApplicationEventPublisher} to delegate to; must not be {@code null}
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher)
    {
        this.publisher = publisher;
    }

    public void publish(Object event)
    {
        if (publisher == null)
        {
            throw new IllegalStateException("LocalEventBus not initialized");
        }
        publisher.publishEvent(event);
    }
}