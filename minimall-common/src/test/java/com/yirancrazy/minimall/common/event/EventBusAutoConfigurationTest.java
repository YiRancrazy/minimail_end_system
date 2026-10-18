package com.yirancrazy.minimall.common.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: EventBusAutoConfigurationTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class EventBusAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
        .withUserConfiguration(TestConfig.class);


    /**
     * 执行 default_eventbus_is_local 方法。
     */
    public void default_eventbus_is_local() {
        runner.run(ctx -> {
            Object bean = ctx.getBean(EventBus.class);
            assertThat(bean).isInstanceOf(LocalEventBus.class);
        });
    }

    /**
     * @Author: yirancrazy@gmail.com
     * @Description: TestConfig 配置类。
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
