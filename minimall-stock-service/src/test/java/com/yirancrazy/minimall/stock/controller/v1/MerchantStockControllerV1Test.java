package com.yirancrazy.minimall.stock.controller.v1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockJournalVo;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用独立 MockMvc 验证商户库存接口的请求头、参数透传和 CSV 导出边界。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MerchantStockControllerV1Test {

  private static final long MERCHANT_ID = 1001L;

  private MockMvc mockMvc;
  private StockService stockService;

  /**
   * 测试前置：创建 StockService Mock，并构建商户库存控制器的独立 MockMvc。
   */
  @BeforeEach
  void setUp() {
    stockService = mock(StockService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new MerchantStockControllerV1(stockService)).build();
  }

  /** * 目标验证商户查询；前置服务对 SKU 99 和商户 1001 返回 100；步骤带商户头查询；预期 200、码 00000、data 为 100. */
  @Test
  void testGetReturnsAvailableQuantity() throws Exception {
    when(stockService.query(99L, MERCHANT_ID)).thenReturn(100L);
    mockMvc
        .perform(get("/api/v1/merchant/stock/99").header("X-Merchant-Id", MERCHANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").value(100));
  }

  /** * 目标验证商户头校验；前置为请求不含 X-Merchant-Id；步骤查询库存；预期返回 400. */
  @Test
  void testGetWithoutMerchantHeaderReturns400() throws Exception {
    mockMvc.perform(get("/api/v1/merchant/stock/99")).andExpect(status().isBadRequest());
  }

  /** * 目标验证阈值透传；前置为 Mock 服务和阈值 50；步骤带商户头设置阈值；预期 200、码 00000 且参数为 99、50、1001. */
  @Test
  void testSetThresholdInvokesService() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/stock/99/threshold")
                .header("X-Merchant-Id", MERCHANT_ID)
                .param("threshold", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(stockService).setThreshold(99L, 50L, MERCHANT_ID);
  }

  /** * 目标验证调整透传；前置为 Mock 服务、数量 10 和原因补货；步骤带商户头调整；预期 200、码 00000 且参数完整. */
  @Test
  void testAdjustStockPassesQuantityAndReason() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/merchant/stock/99/adjust")
                .header("X-Merchant-Id", MERCHANT_ID)
                .param("quantity", "10")
                .param("reason", "补货"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(stockService).adjustStock(99L, 10L, "补货", MERCHANT_ID);
  }

  /** * 目标验证流水查询序列化；前置服务返回 ID 1 流水；步骤带商户头查询；预期 200、码 00000、首条 ID 为 1. */
  @Test
  void testQueryJournalReturnsList() throws Exception {
    StockJournalVo vo = new StockJournalVo();
    vo.setId(1L);
    vo.setSkuId(99L);
    vo.setQuantity(10L);
    vo.setType(1);
    when(stockService.queryJournalVo(99L, MERCHANT_ID)).thenReturn(List.of(vo));
    mockMvc
        .perform(get("/api/v1/merchant/stock/99/journal").header("X-Merchant-Id", MERCHANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data[0].id").value(1));
  }

  /** * 目标验证空流水响应；前置服务返回空集合；步骤带商户头查询；预期 200、码 00000、data 为数组. */
  @Test
  void testQueryJournalReturnsEmptyListWhenNoRecords() throws Exception {
    when(stockService.queryJournalVo(99L, MERCHANT_ID)).thenReturn(Collections.emptyList());
    mockMvc
        .perform(get("/api/v1/merchant/stock/99/journal").header("X-Merchant-Id", MERCHANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").isArray());
  }

  /** * 目标验证流水导出归属；前置服务返回一条流水；步骤带商户头导出；预期 200 且服务收到 SKU 99、商户 1001. */
  @Test
  void testExportJournalPassesMerchantId() throws Exception {
    StockJournalVo vo = new StockJournalVo();
    vo.setId(1L);
    vo.setSkuId(99L);
    vo.setQuantity(10L);
    vo.setType(1);
    when(stockService.exportJournalVo(99L, MERCHANT_ID)).thenReturn(List.of(vo));
    mockMvc
        .perform(
            get("/api/v1/merchant/stock/99/journal/export").header("X-Merchant-Id", MERCHANT_ID))
        .andExpect(status().isOk());
    verify(stockService).exportJournalVo(99L, MERCHANT_ID);
  }

  /** * 目标验证导出商户头校验；前置为请求不含 X-Merchant-Id；步骤请求导出；预期返回 400. */
  @Test
  void testExportJournalRejectsMissingMerchantHeader() throws Exception {
    mockMvc
        .perform(get("/api/v1/merchant/stock/99/journal/export"))
        .andExpect(status().isBadRequest());
  }
}
