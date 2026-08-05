package com.yirancrazy.minimall.pay.controller.v1;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;
import com.yirancrazy.minimall.pay.vo.WithdrawVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantPayControllerV1 MockMvc 单元测试，验证商家端资金流水分页、统计与提现申请 HTTP 路由。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class MerchantPayControllerV1Test {

    private MockMvc mockMvc;
    private PayService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(PayService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantPayControllerV1(service)).build();
    }

    /**
     * 验证 GET /api/v1/merchant/pay/transactions 带 X-Merchant-Id 返回游标分页结果。
     */
    @Test
    void transactions_returns_cursor_result() throws Exception {
        when(service.page(anyLong(), any(PayPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/pay/transactions").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 GET /api/v1/merchant/pay/statistics 带 X-Merchant-Id 返回资金统计 VO。
     */
    @Test
    void statistics_returns_vo() throws Exception {
        PayStatisticsVO vo = new PayStatisticsVO(BigDecimal.TEN, BigDecimal.ZERO, 5L);
        when(service.statistics(anyLong(), any(PayPageDTO.class))).thenReturn(vo);
        mockMvc.perform(get("/api/v1/merchant/pay/statistics").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.transactionCount").value(5));
    }

    /**
     * 验证 POST /api/v1/merchant/pay/withdraw 带 X-Merchant-Id 调用 service 并返回提现 VO。
     */
    @Test
    void applyWithdraw_returns_vo() throws Exception {
        WithdrawVO vo = new WithdrawVO();
        vo.setWithdrawNo("W001");
        when(service.applyWithdraw(anyLong(), any())).thenReturn(vo);
        WithdrawApplyDTO dto = new WithdrawApplyDTO(new BigDecimal("100.00"), "monthly");
        mockMvc.perform(post("/api/v1/merchant/pay/withdraw")
                .header("X-Merchant-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.withdrawNo").value("W001"));
        verify(service).applyWithdraw(anyLong(), any());
    }

    /**
     * 验证 GET /api/v1/merchant/pay/withdrawals 带 X-Merchant-Id 返回提现游标分页结果。
     */
    @Test
    void withdrawals_returns_cursor_result() throws Exception {
        when(service.pageWithdraw(anyLong(), any(PayPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/pay/withdrawals").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }
}
