package com.yirancrazy.minimall.order.controller.v1;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantOrderControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class MerchantOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantOrderControllerV1(service)).build();
    }


    @Test
    void page_returns_cursor_result() throws Exception {
        when(service.merchantPageVO(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/orders").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }


    @Test
    void ship_invokes_service() throws Exception {
        String body = "{\"carrier\":\"顺丰\",\"trackingNo\":\"SF12345678\"}";
        mockMvc.perform(post("/api/v1/merchant/orders/99/ship")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).ship(eq(99L), eq(1L), eq("顺丰"), eq("SF12345678"));
    }


    @Test
    void close_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/orders/99/close").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).merchantClose(99L, 1L);
    }


    @Test
    void pending_count_returns_long() throws Exception {
        when(service.pendingCount(anyLong())).thenReturn(5L);
        mockMvc.perform(get("/api/v1/merchant/orders/_count").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(5));
    }


    @Test
    void refund_review_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/orders/99/refund-review")
                .header("X-Merchant-Id", 1L)
                .param("approved", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).reviewRefund(99L, true, 1L, null);
    }
}
