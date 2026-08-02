package com.yirancrazy.minimall.common.event;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线自动配置，按 rocketmq.enabled 决定注册 LocalEventBus 或 RocketMqEventBus。
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Configuration
public class EventBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LocalEventBus.class)
    public LocalEventBus localEventBus() {
        return new LocalEventBus();
    }

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

    /**
     * Register the RocketMQ-backed {@link EventBus} when rocketmq is enabled.
     * Marked {@link Primary} so it overrides the local fallback in contexts
     * that opt in to transactional messaging.
     *
     * @param namesrvAddr the RocketMQ name server address
     * @param topic the event topic
     * @param outboxProvider optional {@link OutboxStore} for crash-safe callback
     * @return a configured {@link RocketMqEventBus}
     */
    @Bean
    @ConditionalOnProperty(name = "minimall.eventbus.rocketmq.enabled", havingValue = "true")
    @Primary
    public RocketMqEventBus rocketMqEventBus(
        @Value("${minimall.eventbus.rocketmq.namesrv-addr:127.0.0.1:9876}") String namesrvAddr,
        @Value("${minimall.eventbus.rocketmq.topic:minimall-events}") String topic,
        ObjectProvider<OutboxStore> outboxProvider) {
        RocketMqEventBus bus = new RocketMqEventBus(namesrvAddr, topic);
        OutboxStore store = outboxProvider.getIfAvailable();
        if (store != null) {
            bus.setOutboxStore(store);
        }
        return bus;
    }
}
