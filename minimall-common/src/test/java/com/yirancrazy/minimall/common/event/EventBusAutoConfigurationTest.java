package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 事件总线自动装配单元测试，基于 WebApplicationContextRunner 验证在无其他实现时默认暴露的 EventBus 为本地实现。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public class EventBusAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

    /**
     * 验证未配置其他实现时，容器中注入的 EventBus 默认为 LocalEventBus。
     */
    @Test
    public void default_eventbus_is_local() {
        runner.run(ctx -> {
            Object bean = ctx.getBean(EventBus.class);
            assertThat(bean).isInstanceOf(LocalEventBus.class);
        });
    }

    @Configuration
    static class TestConfig {
        @Bean
        LocalEventBus localEventBus() {
            return new LocalEventBus();
        }
    }
}
