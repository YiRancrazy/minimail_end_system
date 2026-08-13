package com.yirancrazy.minimall.common.filter;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: TraceIdFilter 单元测试，验证 traceId 的生成、透传与 MDC 清理。
 * @Version: 1.0
 * @DateTime: 2026/08/13
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void missingHeader_generatesTraceId_andCleansMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertNotNull(MDC.get(Result.TRACE_ID_KEY));
            assertNotNull(((MockHttpServletResponse) res).getHeader(TraceIdFilter.HEADER));
        });

        assertNull(MDC.get(Result.TRACE_ID_KEY));
    }

    @Test
    void incomingHeader_isPreservedInMdcAndResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER, "incoming-tid");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
            assertEquals("incoming-tid", MDC.get(Result.TRACE_ID_KEY)));

        assertEquals("incoming-tid", response.getHeader(TraceIdFilter.HEADER));
        assertNull(MDC.get(Result.TRACE_ID_KEY));
    }
}
