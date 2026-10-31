package com.yirancrazy.minimall.stock.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.stock.dto.StockCorrectDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCompleteDto;
import com.yirancrazy.minimall.stock.dto.StockCountTaskCreateDto;
import com.yirancrazy.minimall.stock.dto.StockPageDto;
import com.yirancrazy.minimall.stock.dto.StockTransferDto;
import com.yirancrazy.minimall.stock.service.StockService;
import com.yirancrazy.minimall.stock.vo.StockStatisticsVo;
import com.yirancrazy.minimall.stock.vo.StockVo;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用独立 MockMvc 验证平台库存、调拨、盘点和纠正接口的契约。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PlatformStockControllerV1Test {

  private MockMvc mockMvc;
  private StockService stockService;
  private final ObjectMapper mapper = new ObjectMapper();

  /**
   * 测试前置：创建 StockService Mock，并构建平台库存控制器的独立 MockMvc。
   */
  @BeforeEach
  void setUp() {
    stockService = mock(StockService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PlatformStockControllerV1(stockService)).build();
  }

  /** * 目标验证平台分页契约；前置服务返回空游标页；步骤请求分页；预期 200 且业务码 00000. */
  @Test
  void testPageReturnsCursorResult() throws Exception {
    when(stockService.pageVo(any(StockPageDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/platform/stock/page"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** * 目标验证统计序列化；前置服务返回 10 个 SKU、告警数 3；步骤请求统计；预期 200、码 00000、SKU 数为 10. */
  @Test
  void testStatisticsReturnsVo() throws Exception {
    when(stockService.platformStatistics())
        .thenReturn(new StockStatisticsVo(10L, 1000L, 200L, 3L, new BigDecimal("0.30")));
    mockMvc
        .perform(get("/api/v1/platform/stock/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data.totalSkuCount").value(10));
  }

  /** * 目标验证调拨调用；前置为 Mock 服务和有效调拨体；步骤带用户头提交；预期 200、码 00000 且服务调用一次. */
  @Test
  void testTransferWithHeaderInvokesService() throws Exception {
    StockTransferDto dto = new StockTransferDto();
    dto.setFromSkuId(1L);
    dto.setToSkuId(2L);
    dto.setQuantity(10L);
    dto.setReason("调拨");
    mockMvc
        .perform(
            post("/api/v1/platform/stock/transfer")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(stockService).transfer(any(StockTransferDto.class));
  }

  /** * 目标验证盘点创建响应；前置服务返回任务 ID 99；步骤带用户头创建；预期 200、码 00000、data 为 99. */
  @Test
  void testCreateCountTaskReturnsTaskId() throws Exception {
    when(stockService.createCountTask(any(StockCountTaskCreateDto.class))).thenReturn(99L);
    StockCountTaskCreateDto dto = new StockCountTaskCreateDto();
    dto.setSkuId(1L);
    dto.setRemark("盘点");
    mockMvc
        .perform(
            post("/api/v1/platform/stock/count-tasks")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").value(99));
  }

  /** * 目标验证盘点完成透传；前置任务 99、实盘 100 且服务为 Mock；步骤提交完成；预期 200、码 00000 且调用一次. */
  @Test
  void testCompleteCountTaskInvokesService() throws Exception {
    StockCountTaskCompleteDto dto = new StockCountTaskCompleteDto();
    dto.setActualQuantity(100L);
    dto.setRemark("盘平");
    mockMvc
        .perform(
            post("/api/v1/platform/stock/count-tasks/99/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(stockService).completeCountTask(any(Long.class), any(StockCountTaskCompleteDto.class));
  }

  /** * 目标验证纠正调用；前置 SKU 99、目标量 100 且服务为 Mock；步骤提交纠正；预期 200、码 00000 且调用一次. */
  @Test
  void testCorrectWithHeaderInvokesService() throws Exception {
    StockCorrectDto dto = new StockCorrectDto();
    dto.setCorrectQuantity(100L);
    dto.setReason("纠正");
    mockMvc
        .perform(
            post("/api/v1/platform/stock/99/correct")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(stockService).correctStock(any(Long.class), any(Long.class), any());
  }

  /** * 目标验证异常库存序列化；前置服务返回 ID 1、可用 -5 的记录；步骤请求异常列表；预期 200、码 00000、首条 ID 为 1. */
  @Test
  void testListAbnormalReturnsList() throws Exception {
    StockVo vo = new StockVo();
    vo.setId(1L);
    vo.setSkuId(99L);
    vo.setAvailable(-5L);
    when(stockService.listAbnormalStockVo()).thenReturn(java.util.List.of(vo));
    mockMvc
        .perform(get("/api/v1/platform/stock/abnormal"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data[0].id").value(1));
  }
}
