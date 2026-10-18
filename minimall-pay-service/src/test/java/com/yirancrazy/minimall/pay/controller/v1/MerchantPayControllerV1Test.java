package com.yirancrazy.minimall.pay.controller.v1;

import java.math.BigDecimal;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.pay.dto.PayPageDTO;
import com.yirancrazy.minimall.pay.service.PayService;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantPayControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class MerchantPayControllerV1Test {

    private MockMvc mockMvc;
    private PayService service;

    @BeforeEach
    void setUp() {
        service = mock(PayService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MerchantPayControllerV1(service)).build();
    }


    @Test
    void transactions_returns_cursor_result() throws Exception {
        when(service.pageVO(anyLong(), any(PayPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/pay/transactions").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }


    @Test
    void statistics_returns_vo() throws Exception {
        PayStatisticsVO vo = new PayStatisticsVO(BigDecimal.TEN, BigDecimal.ZERO, 5L);
        when(service.statistics(anyLong(), any(PayPageDTO.class))).thenReturn(vo);
        mockMvc.perform(get("/api/v1/merchant/pay/statistics").header("X-Merchant-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.transactionCount").value(5));
    }
}
