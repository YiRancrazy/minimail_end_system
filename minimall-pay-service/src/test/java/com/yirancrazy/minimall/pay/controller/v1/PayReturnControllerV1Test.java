package com.yirancrazy.minimall.pay.controller.v1;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付宝同步返回页的路由和输出。
 * 使用独立 MockMvc 检查成功页文案、参数展示以及缺省参数时的稳定响应。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PayReturnControllerV1Test {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new PayReturnControllerV1()).build();
  }

  /** 验证同步返回页：带支付单号和金额请求，预期页面展示成功文案及两项参数. */
  @Test
  void testSuccessReturnsSuccessPage() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/pay/success")
                .param("out_trade_no", "PAY123")
                .param("trade_no", "T123")
                .param("total_amount", "899.00"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("支付成功")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("PAY123")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("899.00")));
  }

  /** 验证同步返回页：不传可选参数请求，预期仍返回成功文案且不抛异常. */
  @Test
  void testSuccessWithoutParamsStillReturnsPage() throws Exception {
    mockMvc
        .perform(get("/api/v1/pay/success"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("支付成功")));
  }
}
