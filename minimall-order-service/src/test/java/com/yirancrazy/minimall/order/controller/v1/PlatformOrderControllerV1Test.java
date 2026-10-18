package com.yirancrazy.minimall.order.controller.v1;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformOrderControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class PlatformOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlatformOrderControllerV1(service)).build();
    }


    @Test
    void page_returns_cursor_result() throws Exception {
        when(service.pageVO(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }


    @Test
    void status_returns_integer() throws Exception {
        when(service.getStatus(anyLong())).thenReturn(2);
        mockMvc.perform(get("/api/v1/platform/orders/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(2));
    }


    @Test
    void detail_returns_vo() throws Exception {
        OrderVO vo = new OrderVO();
        vo.setId(99L);
        vo.setUserId(1L);
        vo.setAmount("10");
        vo.setStatus("PAID");
        when(service.platformDetailVO(anyLong())).thenReturn(vo);
        mockMvc.perform(get("/api/v1/platform/orders/99/detail"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(99));
    }


    @Test
    void close_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/platform/orders/99/close"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).platformClose(99L);
    }


    @Test
    void arbitrate_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/platform/orders/99/arbitrate").param("approved", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).arbitrateRefund(99L, true);
    }


    @Test
    void statistics_returns_vo() throws Exception {
        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setTotalOrderCount(10L);
        when(service.statistics(any(OrderPageDTO.class))).thenReturn(vo);
        mockMvc.perform(get("/api/v1/platform/orders/statistics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalOrderCount").value(10));
    }
}
