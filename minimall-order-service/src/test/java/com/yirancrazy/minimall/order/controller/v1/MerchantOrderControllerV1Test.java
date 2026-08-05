package com.yirancrazy.minimall.order.controller.v1;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantOrderControllerV1 MockMvc 单元测试，验证商家端订单 HTTP 路由、可信 Header 注入与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class MerchantOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantOrderControllerV1(service)).build();
    }

    /**
     * 验证 GET /api/v1/merchant/orders 带 X-Merchant-Id 返回游标分页结果。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(service.page(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/orders").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 POST /api/v1/merchant/orders/{id}/ship 带 X-User-Id 调用 service。
     */
    @Test
    void ship_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/orders/99/ship").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).ship(99L, 1L);
    }

    /**
     * 验证 POST /api/v1/merchant/orders/{id}/close 带 X-Merchant-Id 调用 service。
     */
    @Test
    void close_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/orders/99/close").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).merchantClose(99L, 1L);
    }

    /**
     * 验证 GET /api/v1/merchant/orders/_count 返回商家待处理订单数。
     */
    @Test
    void pending_count_returns_long() throws Exception {
        when(service.pendingCount(anyLong())).thenReturn(5L);
        mockMvc.perform(get("/api/v1/merchant/orders/_count").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(5));
    }

    /**
     * 验证 POST /api/v1/merchant/orders/{id}/refund-review?approved=true 调用 service。
     */
    @Test
    void refund_review_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/orders/99/refund-review")
                .header("X-Merchant-Id", 1L)
                .param("approved", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).reviewRefund(99L, true, 1L);
    }
}
