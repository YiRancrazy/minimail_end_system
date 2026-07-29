package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class EventBusAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);

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
