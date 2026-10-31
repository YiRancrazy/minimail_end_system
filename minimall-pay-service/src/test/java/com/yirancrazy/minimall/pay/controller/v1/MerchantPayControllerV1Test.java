package com.yirancrazy.minimall.pay.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.pay.dto.PayPageDto;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVo;
import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证商家支付控制器与 `PayService` 的接口契约。
 * 使用独立 MockMvc 和 Mock Service，覆盖流水游标响应与统计响应，不加载 Spring 容器。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class MerchantPayControllerV1Test {

  private MockMvc mockMvc;
  private PayService service;

  @BeforeEach
  void setUp() {
    service = mock(PayService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new MerchantPayControllerV1(service)).build();
  }

  /** 验证商家流水查询：配置空游标页并携带商家请求头请求，预期返回成功码和空游标页. */
  @Test
  void testTransactionsReturnsCursorResult() throws Exception {
    when(service.pageVo(anyLong(), any(PayPageDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/merchant/pay/transactions").header("X-Merchant-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证商家统计：配置五笔交易的服务结果，预期成功响应中的交易笔数为五. */
  @Test
  void testStatisticsReturnsVo() throws Exception {
    PayStatisticsVo vo = new PayStatisticsVo(BigDecimal.TEN, BigDecimal.ZERO, 5L);
    when(service.statistics(anyLong(), any(PayPageDto.class))).thenReturn(vo);
    mockMvc
        .perform(get("/api/v1/merchant/pay/statistics").header("X-Merchant-Id", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.transactionCount").value(5));
  }
}
