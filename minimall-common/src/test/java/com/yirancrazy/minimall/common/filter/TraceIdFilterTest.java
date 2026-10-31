package com.yirancrazy.minimall.common.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.common.result.Result;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 TraceIdFilter 对缺失和传入 traceId 的处理，以及 MDC 的请求后清理行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();

  /** * 验证补全traceId；请求头缺失；执行过滤器；预期过滤链与响应有值且结束后清理. */
  @Test
  void testMissingHeaderGeneratesTraceIdAndCleansMdc() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> {
          assertNotNull(MDC.get(Result.TRACE_ID_KEY));
          assertNotNull(((MockHttpServletResponse) res).getHeader(TraceIdFilter.HEADER));
        });

    assertNull(MDC.get(Result.TRACE_ID_KEY));
  }

  /** * 验证透传traceId；请求头为incoming-tid；执行过滤器；预期链内与响应均保留且结束后清理. */
  @Test
  void testIncomingHeaderIsPreservedInMdcAndResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, "incoming-tid");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request,
        response,
        (req, res) -> assertEquals("incoming-tid", MDC.get(Result.TRACE_ID_KEY)));

    assertEquals("incoming-tid", response.getHeader(TraceIdFilter.HEADER));
    assertNull(MDC.get(Result.TRACE_ID_KEY));
  }
}
