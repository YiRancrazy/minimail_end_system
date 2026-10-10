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
     * 将进程内 {@link LocalEventBus} 暴露为默认 {@link EventBus} Bean，
     * 当上下文中未注册其他实现时使用。
     *
     * @param local 始终注册的 {@link LocalEventBus} 候选 Bean
     * @return 同一个 {@link LocalEventBus} 实例，用作兜底 {@link EventBus}
     */
    @Bean
    @ConditionalOnMissingBean(EventBus.class)
    public EventBus defaultEventBus(LocalEventBus local) {
        return local;
    }

    /**
     * 启用 rocketmq 时注册基于 RocketMQ 的 {@link EventBus}。
     * 标记为 {@link Primary}，使其在启用事务消息的上下文中覆盖本地兜底实现。
     *
     * @param namesrvAddr RocketMQ NameServer 地址
     * @param topic 事件主题
     * @param outboxProvider 可选的 {@link OutboxStore}，用于崩溃安全的回调
     * @return 配置好的 {@link RocketMqEventBus}
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
