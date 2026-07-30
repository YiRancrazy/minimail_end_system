package com.yirancrazy.minimall.gateway.filter;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import reactor.core.publisher.Mono;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdGlobalFilter 的单元测试类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
class TraceIdGlobalFilterTest {

    @Test
    void passthrough_traceHeader() {
        TraceIdGlobalFilter f = new TraceIdGlobalFilter();
        String provided = UUID.randomUUID().toString();
        ServerWebExchange ex = MockServerWebExchange.from(
            MockServerHttpRequest.get("/x").header("X-Trace-Id", provided).build());
        Mono<Void> result = f.filter(ex, e -> Mono.empty());
        assertNotNull(result);
        assertEquals(provided, ex.getRequest().getHeaders().getFirst("X-Trace-Id"));
    }
}