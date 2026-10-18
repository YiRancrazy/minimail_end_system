package com.yirancrazy.minimall.order.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.order.service.OrderService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantRefundControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class MerchantRefundControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantRefundControllerV1(service)).build();
    }


    @Test
    void execute_refund_invokes_service() throws Exception {
        String body = "{\"refundAmount\":\"5.00\",\"reason\":\"部分退货\"}";
        mockMvc.perform(post("/api/v1/merchant/refunds/R99/execute")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).merchantInitiateRefundByNo("R99", 1L, "5.00", "部分退货");
    }


    @Test
    void approve_refund_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/refunds/R99/approve")
                .header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).reviewRefundByNo("R99", true, 1L, null);
    }


    @Test
    void reject_refund_invokes_service_with_reason() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/refunds/R99/reject")
                .header("X-Merchant-Id", 1L)
                .param("reason", "证据不足"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).reviewRefundByNo("R99", false, 1L, "证据不足");
    }
}
