package com.yirancrazy.minimall.api.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import feign.RequestTemplate;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部令牌 Feign 透传测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class InternalTokenFeignInterceptorTest {

    @Test
    void apply_givenToken_thenHeaderInjected() {
        InternalTokenFeignInterceptor interceptor =
            new InternalTokenFeignInterceptor("test-token");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals("test-token",
            template.headers().get("X-Internal-Token").iterator().next());
    }

    @Test
    void apply_givenBlankToken_thenNoHeaderAdded() {
        InternalTokenFeignInterceptor interceptor =
            new InternalTokenFeignInterceptor("  ");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertNull(template.headers().get("X-Internal-Token"));
    }
}
