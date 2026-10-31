package com.yirancrazy.minimall.pay.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * @Description: 验证平台支付控制器的查询和冻结接口。
 * 使用 Mock Service 与独立 MockMvc，检查游标响应、统计数据及冻结调用转发。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PlatformPayControllerV1Test {

  private MockMvc mockMvc;
  private PayService service;

  @BeforeEach
  void setUp() {
    service = mock(PayService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new PlatformPayControllerV1(service)).build();
  }

  /** 验证平台流水查询：配置空游标页后请求，预期返回成功码和空游标页. */
  @Test
  void testTransactionsReturnsCursorResult() throws Exception {
    when(service.platformPageVo(any(PayPageDto.class)))
        .thenReturn(new CursorPageVo<>(Collections.emptyList(), null, false, 20));
    mockMvc
        .perform(get("/api/v1/platform/pay/transactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
  }

  /** 验证平台统计：配置五笔交易的服务结果，预期成功响应中的交易笔数为五. */
  @Test
  void testStatisticsReturnsVo() throws Exception {
    PayStatisticsVo vo = new PayStatisticsVo(BigDecimal.TEN, BigDecimal.ZERO, 5L);
    when(service.statistics(any(), any(PayPageDto.class))).thenReturn(vo);
    mockMvc
        .perform(get("/api/v1/platform/pay/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.transactionCount").value(5));
  }

  /** 验证平台冻结：以指定支付单号请求冻结，预期成功且服务只收到该单号. */
  @Test
  void testFreezeInvokesService() throws Exception {
    mockMvc
        .perform(post("/api/v1/platform/pay/P001/freeze"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
    verify(service).freeze("P001");
  }
}
