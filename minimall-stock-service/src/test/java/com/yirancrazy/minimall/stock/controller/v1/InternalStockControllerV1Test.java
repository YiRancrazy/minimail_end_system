package com.yirancrazy.minimall.stock.controller.v1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用独立 MockMvc 和 Mock 服务验证内部库存接口的参数绑定与校验边界。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class InternalStockControllerV1Test {

  private MockMvc mockMvc;
  private StockService stockService;

  /**
   * 测试前置：创建 StockService Mock，并构建不启动 Spring 容器的独立 MockMvc。
   */
  @BeforeEach
  void setUp() {
    stockService = mock(StockService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new InternalStockControllerV1(stockService)).build();
  }

  /** * 目标验证调整参数透传；前置为 Mock 服务；步骤发调整请求；预期 200、码 00000 且四参数一致. */
  @Test
  void testAdjustPassesRequestToService() throws Exception {
    mockMvc
        .perform(
            post("/internal/stock/adjust")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"skuId\":99,\"merchantId\":1001,\"quantity\":5,"
                        + "\"reason\":\"spu edit\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));

    verify(stockService).adjustStock(99L, 5L, "spu edit", 1001L);
  }

  /** * 目标验证批量预占透传；前置为 Mock 服务和两条明细；步骤发批量请求；预期 200、码 00000 且顺序字段一致. */
  @Test
  void testReserveBatchPassesItemsToService() throws Exception {
    mockMvc
        .perform(
            post("/internal/stock/reserve-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"items\":[{\"skuId\":99,\"quantity\":2,\"orderNo\":\"OD99\"},"
                        + "{\"skuId\":100,\"quantity\":3,\"orderNo\":\"OD100\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));

    verify(stockService)
        .reserveBatch(
            org.mockito.ArgumentMatchers.argThat(
                items ->
                    items.size() == 2
                        && items.get(0).getSkuId() == 99L
                        && items.get(0).getQuantity() == 2
                        && "OD99".equals(items.get(0).getOrderNo())
                        && items.get(1).getSkuId() == 100L
                        && items.get(1).getQuantity() == 3
                        && "OD100".equals(items.get(1).getOrderNo())));
  }

  /** * 目标验证预占请求校验；前置为 Mock 服务且请求缺订单号；步骤发预占请求；预期 400 且服务零交互. */
  @Test
  void testReserveRejectsMissingOrderNo() throws Exception {
    mockMvc
        .perform(
            post("/internal/stock/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\":99,\"quantity\":2}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stockService);
  }

  /** * 目标验证预占参数透传；前置为 Mock 服务和有效订单号；步骤发预占请求；预期 200、码 00000 且参数为 99、2、OD99. */
  @Test
  void testReservePassesOrderNoToService() throws Exception {
    mockMvc
        .perform(
            post("/internal/stock/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\":99,\"quantity\":2,\"orderNo\":\"OD99\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));

    verify(stockService).reserve(99L, 2, "OD99");
  }

  /** * 目标验证批量预占校验；前置为 Mock 服务且明细缺订单号；步骤发批量请求；预期 400 且服务零交互. */
  @Test
  void testReserveBatchRejectsMissingOrderNo() throws Exception {
    mockMvc
        .perform(
            post("/internal/stock/reserve-batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"items\":[{\"skuId\":99,\"quantity\":2}]}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(stockService);
  }

  /** * 目标验证释放参数透传；前置为 Mock 服务和有效订单号；步骤发释放请求；预期 200、码 00000 且参数为 99、2、OD99. */
  @Test
  void testReleasePassesOrderNoToService() throws Exception {
    mockMvc
        .perform(
            post("/internal/stock/release")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skuId\":99,\"quantity\":2,\"orderNo\":\"OD99\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));

    verify(stockService).release(99L, 2, "OD99");
  }
}
