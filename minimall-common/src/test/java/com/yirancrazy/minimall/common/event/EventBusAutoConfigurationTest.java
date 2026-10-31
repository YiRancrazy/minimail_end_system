package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证事件总线自动配置按 RocketMQ 开关注册实现。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class EventBusAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration.class));


    /**
     * RocketMQ disabled must not create a local fallback EventBus.
     */
    @Test
    public void rocketMqDisabled_doesNotCreateEventBus() {
        runner.withPropertyValues("minimall.eventbus.rocketmq.enabled=false")
            .run(ctx -> assertThat(ctx).doesNotHaveBean(EventBus.class));
    }

    /**
     * Configurations requiring RocketMQ must fail when RocketMQ is disabled.
     */
    @Test
    public void requireRocketMq_failsWhenDisabled() {
        runner.withPropertyValues(
                "minimall.eventbus.require-rocketmq=true",
                "minimall.eventbus.rocketmq.enabled=false")
            .run(ctx -> {
                assertThat(ctx).hasFailed();
                assertThat(ctx.getStartupFailure())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(
                        "minimall.eventbus.rocketmq.enabled must be true when "
                            + "minimall.eventbus.require-rocketmq=true");
            });
    }
}
