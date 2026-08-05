package com.yirancrazy.minimall.order.controller.v1;

import java.math.BigDecimal;
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
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformOrderControllerV1 MockMvc 单元测试，验证平台端订单 HTTP 路由、状态查询与退款仲裁状态流转。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class PlatformOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlatformOrderControllerV1(service)).build();
    }

    /**
     * 验证 GET /api/v1/platform/orders 返回游标分页结果。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(service.page(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/platform/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 GET /api/v1/platform/orders/{id} 返回订单状态码。
     */
    @Test
    void status_returns_integer() throws Exception {
        when(service.getStatus(anyLong())).thenReturn(2);
        mockMvc.perform(get("/api/v1/platform/orders/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(2));
    }

    /**
     * 验证 GET /api/v1/platform/orders/{id}/detail 调用 service 并返回 VO。
     */
    @Test
    void detail_returns_vo() throws Exception {
        OrderPO po = new OrderPO();
        po.setId(99L);
        po.setUserId(1L);
        po.setAmount(BigDecimal.TEN);
        po.setStatus(2);
        when(service.getDetail(anyLong())).thenReturn(po);
        mockMvc.perform(get("/api/v1/platform/orders/99/detail"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(99));
    }

    /**
     * 验证 POST /api/v1/platform/orders/{id}/close 调用 service 强制关单。
     */
    @Test
    void close_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/platform/orders/99/close"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).platformClose(99L);
    }

    /**
     * 验证 POST /api/v1/platform/orders/{id}/arbitrate?approved=true 调用 service 退款仲裁。
     */
    @Test
    void arbitrate_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/platform/orders/99/arbitrate").param("approved", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).arbitrateRefund(99L, true);
    }

    /**
     * 验证 GET /api/v1/platform/orders/statistics 返回订单统计 VO。
     */
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
