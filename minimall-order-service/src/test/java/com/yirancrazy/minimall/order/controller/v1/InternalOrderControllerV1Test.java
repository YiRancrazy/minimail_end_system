package com.yirancrazy.minimall.order.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDTO;
import com.yirancrazy.minimall.order.service.OrderService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalOrderControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class InternalOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalOrderControllerV1(service)).build();
    }


    @Test
    void commentEligibility_returns_snapshot() throws Exception {
        when(service.commentEligibility(eq("OD10001"), anyLong(), anyLong()))
            .thenReturn(new OrderCommentEligibilityDTO(true, 99L, 7L, 11L, 4));

        mockMvc.perform(get("/internal/order/comment-eligibility")
                .param("orderNo", "OD10001")
                .param("userId", "7")
                .param("spuId", "500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.eligible").value(true))
            .andExpect(jsonPath("$.data.merchantId").value(11));

        verify(service).commentEligibility("OD10001", 7L, 500L);
    }


    @Test
    void commentEligibility_returns_not_eligible() throws Exception {
        when(service.commentEligibility(eq("OD10002"), anyLong(), anyLong()))
            .thenReturn(new OrderCommentEligibilityDTO(false, 100L, 8L, null, 1));

        mockMvc.perform(get("/internal/order/comment-eligibility")
                .param("orderNo", "OD10002")
                .param("userId", "8")
                .param("spuId", "600"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.eligible").value(false));
    }
}
