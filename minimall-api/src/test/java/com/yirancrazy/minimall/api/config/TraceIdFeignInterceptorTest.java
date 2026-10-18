package com.yirancrazy.minimall.api.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import feign.RequestTemplate;
import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdFeignInterceptorTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class TraceIdFeignInterceptorTest {

    private TraceIdFeignInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TraceIdFeignInterceptor();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void apply_givenMdcTraceId_thenHeaderInjected() {
        MDC.put(Result.TRACE_ID_KEY, "tid-123");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertEquals("tid-123", template.headers().get(TraceIdFilter.HEADER).iterator().next());
    }

    @Test
    void apply_givenBlankMdc_thenNoHeaderAdded() {
        MDC.put(Result.TRACE_ID_KEY, "  ");
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertNull(template.headers().get(TraceIdFilter.HEADER));
    }
}
