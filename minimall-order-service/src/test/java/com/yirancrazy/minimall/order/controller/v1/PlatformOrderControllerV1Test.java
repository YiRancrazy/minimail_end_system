package com.yirancrazy.minimall.order.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.yirancrazy.minimall.order.vo.OrderStatisticsVo;
import com.yirancrazy.minimall.order.vo.OrderVo;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证平台订单控制器的分页、状态、详情、关闭、仲裁和统计响应。
 *   业务边界：使用 standalone MockMvc 与 OrderService 桩，不访问数据库或远程服务。
 *   使用场景：平台订单 API 的控制器单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PlatformOrderControllerV1Test {

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
    mockMvc = MockMvcBuilders.standaloneSetup(new PlatformOrderControllerV1(service)).build();
  }

  /** 验证分页返回游标结果. */
  @Test
  void testPageReturnsCursorResult() throws Exception {
    when(service.pageVo(any(OrderPageDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/platform/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证状态返回整数. */
  @Test
  void testStatusReturnsInteger() throws Exception {
    when(service.getStatus(anyLong())).thenReturn(2);
    mockMvc
        .perform(get("/api/v1/platform/orders/99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(2));
  }

  /** 验证详情返回视图对象. */
  @Test
  void testDetailReturnsVo() throws Exception {
    OrderVo vo = new OrderVo();
    vo.setId(99L);
    vo.setUserId(1L);
    vo.setAmount("10");
    vo.setStatus("PAID");
    when(service.platformDetailVo(anyLong())).thenReturn(vo);
    mockMvc
        .perform(get("/api/v1/platform/orders/99/detail"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(99));
  }

  /** 验证商家关闭订单调用订单服务. */
  @Test
  void testCloseInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/platform/orders/99/close"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).platformClose(99L);
  }

  /** 验证平台退款仲裁调用订单服务. */
  @Test
  void testArbitrateInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/platform/orders/99/arbitrate").param("approved", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).arbitrateRefund(99L, true);
  }

  /** 验证平台统计接口返回订单统计视图. */
  @Test
  void testStatisticsReturnsVo() throws Exception {
    OrderStatisticsVo vo = new OrderStatisticsVo();
    vo.setTotalOrderCount(10L);
    when(service.statistics(any(OrderPageDto.class))).thenReturn(vo);
    mockMvc
        .perform(get("/api/v1/platform/orders/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalOrderCount").value(10));
  }
}
