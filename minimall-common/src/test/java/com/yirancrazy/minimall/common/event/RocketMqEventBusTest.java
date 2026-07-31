package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RocketMQ EventBus 条件装配测试。验证在启用 RocketMQ 配置时，
 * EventBus 实现为 RocketMqEventBus。使用虚拟 namesrv 地址以避免依赖真实 broker。
 */
public class RocketMqEventBusTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withConfiguration(
            org.springframework.boot.autoconfigure.AutoConfigurations.of(EventBusAutoConfiguration.class))
        .withBean(LocalEventBus.class)
        .withBean(RocketMqEventBus.class, () -> new RocketMqEventBus("127.0.0.1:9877", "minimall-events"))
        .withPropertyValues(
            "minimall.eventbus.rocketmq.enabled=true",
            "minimall.eventbus.rocketmq.namesrv-addr=127.0.0.1:9877"
        );

    /**
     * 验证当 minimall.eventbus.rocketmq.enabled=true 时，
     * 容器注入的 EventBus 为 RocketMqEventBus 实例。
     */
    @Test
    public void shouldUseRocketMqEventBus() {
        runner.run(ctx -> {
            assertThat(ctx.getBean(RocketMqEventBus.class)).isInstanceOf(EventBus.class);
        });
    }
}