package com.yirancrazy.minimall.order.controller.v1;

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

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.service.OrderService;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证商家订单控制器的分页、发货、关闭、计数和退款审核 HTTP 契约。
 *   业务边界：使用 standalone MockMvc 与 OrderService 桩，通过请求头和 JSON 体检查服务调用参数。
 *   使用场景：商家订单 API 的控制器单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MerchantOrderControllerV1Test {

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
    mockMvc = MockMvcBuilders.standaloneSetup(new MerchantOrderControllerV1(service)).build();
  }

  /** 验证分页返回游标结果. */
  @Test
  void testPageReturnsCursorResult() throws Exception {
    when(service.merchantPageVo(any(OrderPageDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/merchant/orders").header("X-Merchant-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证发货调用服务. */
  @Test
  void testShipInvokesService() throws Exception {
    String body = "{\"carrier\":\"顺丰\",\"trackingNo\":\"SF12345678\"}";
    mockMvc
        .perform(
            post("/api/v1/merchant/orders/99/ship")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).ship(eq(99L), eq(1L), eq("顺丰"), eq("SF12345678"));
  }

  /** 验证商家关闭订单调用订单服务. */
  @Test
  void testCloseInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/merchant/orders/99/close").header("X-Merchant-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).merchantClose(99L, 1L);
  }

  /** 验证商家待办订单计数按长整型返回. */
  @Test
  void testPendingCountReturnsLong() throws Exception {
    when(service.pendingCount(anyLong())).thenReturn(5L);
    mockMvc
        .perform(get("/api/v1/merchant/orders/_count").header("X-Merchant-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(5));
  }

  /** 验证退款审核调用服务. */
  @Test
  void testRefundReviewInvokesService() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/orders/99/refund-review")
                .header("X-Merchant-Id", 1L)
                .param("approved", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).reviewRefund(99L, true, 1L, null);
  }
}
