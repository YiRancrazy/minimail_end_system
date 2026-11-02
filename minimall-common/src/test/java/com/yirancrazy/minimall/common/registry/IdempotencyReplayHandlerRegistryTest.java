package com.yirancrazy.minimall.common.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.handle.IdempotencyReplayHandler;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证幂等重放处理器按业务类型路由并拒绝重复注册。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:10
 */
class IdempotencyReplayHandlerRegistryTest {

    /** * 验证不同业务类型可以路由到各自处理器. */
    @Test
    void testRoutesByReplayType() {
        IdempotencyReplayHandler checkout = mock(IdempotencyReplayHandler.class);
        IdempotencyReplayHandler payment = mock(IdempotencyReplayHandler.class);
        when(checkout.replayType()).thenReturn("order:checkout");
        when(payment.replayType()).thenReturn("pay:create");

        IdempotencyReplayHandlerRegistry registry =
                new IdempotencyReplayHandlerRegistry(List.of(checkout, payment));

        assertSame(checkout, registry.find("order:checkout"));
        assertSame(payment, registry.find("pay:create"));
        assertNull(registry.find("pay:refund"));
    }

    /** * 验证重复业务类型会在注册阶段失败. */
    @Test
    void testRejectsDuplicateReplayType() {
        IdempotencyReplayHandler first = mock(IdempotencyReplayHandler.class);
        IdempotencyReplayHandler second = mock(IdempotencyReplayHandler.class);
        when(first.replayType()).thenReturn("same-type");
        when(second.replayType()).thenReturn("same-type");

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                new IdempotencyReplayHandlerRegistry(
                                        List.of(first, second)));

        assertEquals("重复的幂等重放业务类型: same-type", exception.getMessage());
    }
}
