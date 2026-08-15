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
 * @Description: PayReturnControllerV1 单元测试，验证支付宝同步跳转落地页返回支付成功页面。
 * @Version: 1.0
 * @DateTime: 2026/08/15
 **/
class PayReturnControllerV1Test {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PayReturnControllerV1()).build();
    }

    /**
     * 验证 GET /api/v1/pay/success 返回 200 且包含支付成功提示。
     */
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

    /**
     * 验证参数缺失时页面仍正常返回，不抛异常。
     */
    @Test
    void success_without_params_still_returns_page() throws Exception {
        mockMvc.perform(get("/api/v1/pay/success"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("支付成功")));
    }
}
