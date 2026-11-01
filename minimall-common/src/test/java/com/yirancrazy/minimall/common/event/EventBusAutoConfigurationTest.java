package com.yirancrazy.minimall.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用 ApplicationContextRunner 验证事件总线自动配置的开关和启动失败语义。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class EventBusAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(EventBusAutoConfiguration.class));

    /** * 验证关闭RocketMQ不建Bean；仅启用自动配置；启动上下文；预期无事件总线Bean. */
    @Test
    public void testRocketMqDisabledDoesNotCreateEventBus() {
        runner
                .withPropertyValues("minimall.eventbus.rocketmq.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(EventBus.class));
    }

    /** * 验证配置冲突启动失败；要求启用但实际关闭；启动上下文；预期状态异常且消息指出冲突. */
    @Test
    public void testRequireRocketMqFailsWhenDisabled() {
        runner
                .withPropertyValues(
                        "minimall.eventbus.require-rocketmq=true", "minimall.eventbus.rocketmq.enabled=false")
                .run(
                        ctx -> {
                            assertThat(ctx).hasFailed();
                            assertThat(ctx.getStartupFailure())
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessage(
                                            "minimall.eventbus.rocketmq.enabled must be true when "
                                                    + "minimall.eventbus.require-rocketmq=true");
                        });
    }
}
