package com.yirancrazy.minimall.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.yirancrazy.minimall.common.aspect.IdempotencyAspect;
import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import com.yirancrazy.minimall.common.registry.IdempotencyReplayHandlerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @Author: YiRanCrazy@gmail.com
 * @Description: 验证幂等切面自动配置显式注册重放处理器注册表。
 * @Version: 1.0
 * @DateTime: 2026/11/2 08:40
 **/
class IdempotencyAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(IdempotencyAutoConfiguration.class));

    /** * 验证自动配置注册注册表和切面并从注册表查询业务处理器. */
    @Test
    void testRegistryAndAspectAreRegistered() {
        runner.withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(IdempotencyReplayHandler.class, TestReplayHandler::new)
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(IdempotencyReplayHandlerRegistry.class);
                            assertThat(context).hasSingleBean(IdempotencyAspect.class);
                            assertThat(
                                            context.getBean(IdempotencyReplayHandlerRegistry.class)
                                                    .find(TestReplayHandler.REPLAY_TYPE))
                                    .isInstanceOf(TestReplayHandler.class);
                        });
    }

    /**
     * 测试用幂等重放处理器。
     */
    private static final class TestReplayHandler implements IdempotencyReplayHandler {

        private static final String REPLAY_TYPE = "test";

        /**
         * 返回测试业务类型。
         *
         * @return 测试业务类型
         */
        @Override
        public String replayType() {
            return REPLAY_TYPE;
        }

        /**
         * 测试场景不需要结果回放。
         *
         * @param context 幂等上下文
         * @return null
         */
        @Override
        public Object onDuplicate(IdempotencyContext context) {
            return null;
        }
    }
}
