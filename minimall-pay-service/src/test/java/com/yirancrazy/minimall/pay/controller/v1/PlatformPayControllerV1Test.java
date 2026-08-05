package com.yirancrazy.minimall.pay.controller.v1;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformPayControllerV1 MockMvc 单元测试，验证平台端交易流水分页、资金统计、冻结与提现审核 HTTP 路由。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class PlatformPayControllerV1Test {

    private MockMvc mockMvc;
    private PayService service;

    @BeforeEach
    void setUp() {
        service = mock(PayService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlatformPayControllerV1(service)).build();
    }

    /**
     * 验证 GET /api/v1/platform/pay/transactions 返回游标分页结果。
     */
    @Test
    void transactions_returns_cursor_result() throws Exception {
        when(service.platformPage(any(PayPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/pay/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 GET /api/v1/platform/pay/statistics 返回全平台资金统计 VO。
     */
    @Test
    void statistics_returns_vo() throws Exception {
        PayStatisticsVO vo = new PayStatisticsVO(BigDecimal.TEN, BigDecimal.ZERO, 5L);
        when(service.statistics(any(), any(PayPageDTO.class))).thenReturn(vo);
        mockMvc.perform(get("/api/v1/platform/pay/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.transactionCount").value(5));
    }

    /**
     * 验证 POST /api/v1/platform/pay/{paymentNo}/freeze 调用 service 冻结支付单。
     */
    @Test
    void freeze_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/platform/pay/P001/freeze"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).freeze("P001");
    }

    /**
     * 验证 GET /api/v1/platform/pay/withdrawals 返回提现游标分页结果。
     */
    @Test
    void withdrawals_returns_cursor_result() throws Exception {
        when(service.platformPageWithdraw(any(PayPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/pay/withdrawals"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 POST /api/v1/platform/pay/withdrawals/{id}/review?approved=true 调用 service 审核提现。
     */
    @Test
    void reviewWithdraw_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/platform/pay/withdrawals/1/review")
                .param("approved", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).reviewWithdraw(1L, true, null);
    }
}
