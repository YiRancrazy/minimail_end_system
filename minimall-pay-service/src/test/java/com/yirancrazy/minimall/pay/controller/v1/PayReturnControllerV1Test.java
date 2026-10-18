package com.yirancrazy.minimall.pay.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayReturnControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class PayReturnControllerV1Test {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PayReturnControllerV1()).build();
    }


    @Test
    void success_returns_success_page() throws Exception {
        mockMvc.perform(get("/api/v1/pay/success")
                .param("out_trade_no", "PAY123")
                .param("trade_no", "T123")
                .param("total_amount", "899.00"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("支付成功")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("PAY123")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("899.00")));
    }


    @Test
    void success_without_params_still_returns_page() throws Exception {
        mockMvc.perform(get("/api/v1/pay/success"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("支付成功")));
    }
}
