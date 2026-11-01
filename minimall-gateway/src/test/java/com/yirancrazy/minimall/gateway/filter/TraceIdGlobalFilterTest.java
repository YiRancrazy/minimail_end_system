package com.yirancrazy.minimall.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证链路标识过滤器能够接受已携带 X-Trace-Id 的请求。测试使用模拟 HTTP 交换对象
 *   和透传过滤器链，不启动网关。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class TraceIdGlobalFilterTest {

    /** 验证已有链路标识可透传；前置为请求头含 UUID；执行过滤器与空链；预期返回非空且原请求头保留. */
    @Test
    void testPassthroughTraceHeader() {
        TraceIdGlobalFilter f = new TraceIdGlobalFilter();
        String provided = UUID.randomUUID().toString();
        ServerWebExchange ex =
                MockServerWebExchange.from(
                        MockServerHttpRequest.get("/x").header("X-Trace-Id", provided).build());
        Mono<Void> result = f.filter(ex, e -> Mono.empty());
        assertNotNull(result);
        assertEquals(provided, ex.getRequest().getHeaders().getFirst("X-Trace-Id"));
    }
}
