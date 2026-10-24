package com.yirancrazy.minimall.cart.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车内部接口访问过滤器测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class InternalAuthFilterTest {

    private InternalAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalAuthFilter("test-token", new ObjectMapper());
    }

    @Test
    void internalPath_rejectsMissingToken() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/internal/cart/count");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void publicPath_skipsInternalCheck() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest("GET", "/api/v1/user/cart");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void internalPath_acceptsValidToken() throws Exception {
        MockHttpServletRequest request =
            new MockHttpServletRequest("POST", "/internal/cart/count");
        request.addHeader("X-Internal-Token", "test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }
}
