package com.yirancrazy.minimall.order.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDTO;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDTO;
import com.yirancrazy.minimall.order.dto.OrderCreateDTO;
import com.yirancrazy.minimall.order.dto.OrderPageDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.service.OrderService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserOrderControllerV1 MockMvc 单元测试，验证 HTTP 层路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
class UserOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserOrderControllerV1(service)).build();
    }

    /**
     * 验证 GET /api/v1/user/orders 返回游标分页结果。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(service.page(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/user/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }

    /**
     * 验证 GET /api/v1/user/orders/status-counts 带 X-User-Id 头返回各状态订单数量。
     */
    @Test
    void statusCounts_returns_counts() throws Exception {
        com.yirancrazy.minimall.order.vo.OrderStatusCountsVO vo =
            new com.yirancrazy.minimall.order.vo.OrderStatusCountsVO();
        vo.setPendingCount(2);
        vo.setPaidCount(1);
        vo.setShippedCount(3);
        vo.setCompletedCount(4);
        when(service.countStatusByUser(7L)).thenReturn(vo);

        mockMvc.perform(get("/api/v1/user/orders/status-counts").header("X-User-Id", 7L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.pendingCount").value(2))
            .andExpect(jsonPath("$.data.paidCount").value(1))
            .andExpect(jsonPath("$.data.shippedCount").value(3))
            .andExpect(jsonPath("$.data.completedCount").value(4));
        verify(service).countStatusByUser(7L);
    }

    /**
     * 验证 POST /api/v1/user/orders/checkout 调用 service 并返回 201。
     */
    @Test
    void checkout_returns_created() throws Exception {
        when(service.checkout(anyLong(), any(), any())).thenReturn(100L);
        OrderCheckoutDTO dto = new OrderCheckoutDTO();
        dto.setItems(java.util.List.of(new OrderCheckoutItemDTO(1L, 2)));
        mockMvc.perform(post("/api/v1/user/orders/checkout")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value(100));
        verify(service).checkout(anyLong(), any(), any());
    }

    /**
     * 验证 POST /api/v1/user/orders 带 X-User-Id 头创建订单并返回 201。
     */
    @Test
    void create_with_header_returns_created() throws Exception {
        when(service.create(anyLong(), anyLong(), anyInt(), any())).thenReturn(100L);
        OrderCreateDTO dto = new OrderCreateDTO(100L, 2, null);
        mockMvc.perform(post("/api/v1/user/orders")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data").value(100));
        verify(service).create(eq(1L), eq(100L), eq(2), any());
    }

    /**
     * 验证 POST /api/v1/user/orders/{id}/pay 带 X-User-Id 调用 service。
     */
    @Test
    void pay_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/orders/99/pay").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).pay(99L, 1L);
    }

    /**
     * 验证 GET /api/v1/user/orders/{id} 带 X-User-Id 返回订单状态。
     */
    @Test
    void status_returns_integer() throws Exception {
        when(service.getStatus(99L, 1L)).thenReturn(2);
        mockMvc.perform(get("/api/v1/user/orders/99").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(2));
    }

    /**
     * 验证 GET /api/v1/user/orders/{id}/detail 带 X-User-Id 调用 service 并返回 VO。
     */
    @Test
    void detail_returns_vo() throws Exception {
        OrderPO po = new OrderPO();
        po.setId(99L);
        po.setUserId(1L);
        po.setAmount(java.math.BigDecimal.TEN);
        po.setStatus(2);
        when(service.getDetail(99L, 1L)).thenReturn(po);
        mockMvc.perform(get("/api/v1/user/orders/99/detail").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(99));
    }

    /**
     * 验证 GET /api/v1/user/orders/{orderId}/logistics 带 X-User-Id 调用 service 并返回物流节点。
     */
    @Test
    void logistics_returns_nodes() throws Exception {
        when(service.queryLogistics(99L, 1L)).thenReturn(java.util.List.of(
            new com.yirancrazy.minimall.order.vo.OrderLogisticsVO()));
        mockMvc.perform(get("/api/v1/user/orders/99/logistics").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).queryLogistics(99L, 1L);
    }

    /**
     * 验证 POST /api/v1/user/orders/{id}/cancel 带 X-User-Id 调用 service。
     */
    @Test
    void cancel_with_header_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/orders/99/cancel").header("X-User-Id", 1L))
            .andExpect(status().isOk());
        verify(service).cancel(99L, 1L);
    }

    /**
     * 验证 POST /api/v1/user/orders/{id}/refund 带 X-User-Id 调用 service。
     */
    @Test
    void refund_with_header_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/orders/99/refund").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).refund(99L, 1L);
    }
}
