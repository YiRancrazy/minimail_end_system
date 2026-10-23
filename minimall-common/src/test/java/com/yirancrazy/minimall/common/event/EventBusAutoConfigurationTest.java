package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证事件总线自动配置默认注册本地事件总线实现。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class EventBusAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);


    /**
     * 验证默认事件总线为本地实现。
     */
    public void default_eventbus_is_local() {
        runner.run(ctx -> {
            Object bean = ctx.getBean(EventBus.class);
            assertThat(bean).isInstanceOf(LocalEventBus.class);
        });
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: 测试用自动配置，提供本地事件总线 Bean。
     * @Version: 1.0
     * @DateTime: 2026/10/16
     **/
    @Configuration
    static class TestConfig {
        @Bean
        LocalEventBus localEventBus() {
            return new LocalEventBus();
        }
    }
}
