package com.yirancrazy.minimall.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验内部令牌仅通过 Feign 请求头透传，空白令牌不写入.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class InternalTokenFeignInterceptorTest {

  /** 非空内部令牌应写入 X-Internal-Token 请求头. */
  @Test
  void testApplyGivenTokenThenHeaderInjected() {
    InternalTokenFeignInterceptor interceptor = new InternalTokenFeignInterceptor("test-token");
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertEquals("test-token", template.headers().get("X-Internal-Token").iterator().next());
  }

  /** 空白内部令牌不应写入请求头. */
  @Test
  void testApplyGivenBlankTokenThenNoHeaderAdded() {
    InternalTokenFeignInterceptor interceptor = new InternalTokenFeignInterceptor("  ");
    RequestTemplate template = new RequestTemplate();

    interceptor.apply(template);

    assertNull(template.headers().get("X-Internal-Token"));
  }
}
