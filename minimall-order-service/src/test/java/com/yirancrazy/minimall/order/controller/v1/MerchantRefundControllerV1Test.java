package com.yirancrazy.minimall.order.controller.v1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证商家退款控制器按退款单号执行、通过和拒绝退款的 HTTP 契约。
 *   业务边界：使用 standalone MockMvc 与 OrderService 桩，重点检查退款原因和商家身份透传。
 *   使用场景：商家退款 API 的控制器单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MerchantRefundControllerV1Test {

  private MockMvc mockMvc;
  private OrderService service;

  /**
   * 初始化当前测试类的共享夹具。
   *
   * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
   */
  @BeforeEach
  void setUp() {
    service = mock(OrderService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new MerchantRefundControllerV1(service)).build();
  }

  /** 验证按退款单号执行退款并委派订单服务. */
  @Test
  void testExecuteRefundInvokesService() throws Exception {
    String body = "{\"refundAmount\":\"5.00\",\"reason\":\"部分退货\"}";
    mockMvc
        .perform(
            post("/api/v1/merchant/refunds/R99/execute")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).merchantInitiateRefundByNo("R99", 1L, "5.00", "部分退货");
  }

  /** 验证按退款单号通过退款并委派订单服务. */
  @Test
  void testApproveRefundInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/merchant/refunds/R99/approve").header("X-Merchant-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).reviewRefundByNo("R99", true, 1L, null);
  }

  /** 验证拒绝退款调用服务使用原因. */
  @Test
  void testRejectRefundInvokesServiceWithReason() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/refunds/R99/reject")
                .header("X-Merchant-Id", 1L)
                .param("reason", "证据不足"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).reviewRefundByNo("R99", false, 1L, "证据不足");
  }
}
