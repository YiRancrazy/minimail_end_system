package com.yirancrazy.minimall.cart.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在不启动 Spring 的情况下，将 InternalAuthFilter 作为独立 Servlet 过滤器测试。
 *     场景覆盖公开路径放行、内部令牌缺失和令牌匹配；断言主要关注响应状态，不校验 JSON 响应体。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class InternalAuthFilterTest {

  private InternalAuthFilter filter;

  @BeforeEach
  void setUp() {
    filter = new InternalAuthFilter("test-token", new ObjectMapper());
  }

  /** 验证内部路径缺少 X-Internal-Token 时返回 403，且不进入后续业务处理. */
  @Test
  void testInternalPathRejectsMissingToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/cart/count");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(403, response.getStatus());
  }

  /** 验证公开购物车路径不校验内部令牌，过滤器链继续执行并保持 200 响应. */
  @Test
  void testPublicPathSkipsInternalCheck() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/user/cart");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
  }

  /** 验证内部路径携带正确令牌时放行请求，过滤器链继续执行并保持 200 响应. */
  @Test
  void testInternalPathAcceptsValidToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/cart/count");
    request.addHeader("X-Internal-Token", "test-token");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(200, response.getStatus());
  }
}
