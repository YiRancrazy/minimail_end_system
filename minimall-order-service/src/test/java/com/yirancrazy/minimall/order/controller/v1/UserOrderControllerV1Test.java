package com.yirancrazy.minimall.order.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserOrderControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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


    @Test
    void page_returns_cursor_result() throws Exception {
        when(service.pageVO(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/user/orders").header("X-User-Id", 7L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
    }


    @Test
    void page_overrides_client_user_id() throws Exception {
        when(service.pageVO(any(OrderPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));

        mockMvc.perform(get("/api/v1/user/orders")
                .header("X-User-Id", 7L)
                .param("userId", "999"))
            .andExpect(status().isOk());

        ArgumentCaptor<OrderPageDTO> captor = ArgumentCaptor.forClass(OrderPageDTO.class);
        verify(service).pageVO(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
    }


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


    @Test
    void pay_endpoint_removed() throws Exception {
        mockMvc.perform(post("/api/v1/user/orders/99/pay").header("X-User-Id", 1L))
            .andExpect(status().isNotFound());
    }


    @Test
    void status_returns_integer() throws Exception {
        when(service.getStatus(99L, 1L)).thenReturn(2);
        mockMvc.perform(get("/api/v1/user/orders/99").header("X-User-Id", 1L))
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
        when(service.detailVO(99L, 1L)).thenReturn(vo);
        mockMvc.perform(get("/api/v1/user/orders/99/detail").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(99));
    }


    @Test
    void logistics_returns_nodes() throws Exception {
        when(service.queryLogistics(99L, 1L)).thenReturn(java.util.List.of(
            new com.yirancrazy.minimall.order.vo.OrderLogisticsVO()));
        mockMvc.perform(get("/api/v1/user/orders/99/logistics").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).queryLogistics(99L, 1L);
    }


    @Test
    void cancel_with_header_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/orders/99/cancel").header("X-User-Id", 1L))
            .andExpect(status().isOk());
        verify(service).cancel(99L, 1L);
    }


    @Test
    void refund_with_header_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/orders/99/refund").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(service).refund(99L, 1L);
    }
}
