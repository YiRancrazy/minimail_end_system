package com.yirancrazy.minimall.common.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: EventBusAutoConfiguration description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class EventBusAutoConfiguration {

    /**
     * Expose the in-process {@link LocalEventBus} as the default {@link EventBus}
     * bean when no other implementation is registered in the context.
     *
     * @param local the always-registered {@link LocalEventBus} candidate bean
     * @return the same {@link LocalEventBus} instance, to be used as the fallback {@link EventBus}
     */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus defaultEventBus(LocalEventBus local) {
        return local;
    }
}
