package com.yirancrazy.minimall.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.common.filter.TraceIdFilter;
import com.yirancrazy.minimall.common.result.Result;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验 Feign 调用仅在 MDC 含有效 TraceId 时透传链路请求头.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class TraceIdFeignInterceptorTest {

  private TraceIdFeignInterceptor interceptor;

  /** 为每个测试创建独立的 TraceId 拦截器. */
  @BeforeEach
  void setUp() {
    interceptor = new TraceIdFeignInterceptor();
  }

  /** 清理 MDC，避免 TraceId 在测试之间泄漏. */
  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  /** 非空 MDC TraceId 应写入 Feign 请求头. */
  @Test
  void testApplyGivenMdcTraceIdThenHeaderInjected() {
    MDC.put(Result.TRACE_ID_KEY, "tid-123");
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertEquals("tid-123", template.headers().get(TraceIdFilter.HEADER).iterator().next());
  }

  /** 空白 MDC TraceId 不应写入请求头. */
  @Test
  void testApplyGivenBlankMdcThenNoHeaderAdded() {
    MDC.put(Result.TRACE_ID_KEY, "  ");
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertNull(template.headers().get(TraceIdFilter.HEADER));
  }
}
