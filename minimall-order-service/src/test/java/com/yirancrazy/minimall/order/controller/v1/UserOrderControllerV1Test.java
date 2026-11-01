package com.yirancrazy.minimall.order.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.order.constant.CheckoutSourceEnum;
import com.yirancrazy.minimall.order.dto.OrderCheckoutDto;
import com.yirancrazy.minimall.order.dto.OrderCheckoutItemDto;
import com.yirancrazy.minimall.order.dto.OrderCreateDto;
import com.yirancrazy.minimall.order.dto.OrderPageDto;
import com.yirancrazy.minimall.order.service.OrderCheckoutService;
import com.yirancrazy.minimall.order.service.OrderService;
import com.yirancrazy.minimall.order.vo.OrderVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证用户订单控制器的分页、结算、创建、详情、取消、退款和物流接口。
 *   业务边界：使用 standalone MockMvc、OrderService 桩和 OrderCheckoutService 桩检查请求头覆盖、状态码与参数传递。
 *   使用场景：用户订单 API 的控制器单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class UserOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;
    private OrderCheckoutService checkoutService;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 初始化当前测试类的共享夹具。
     *
     * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
     */
    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        checkoutService = mock(OrderCheckoutService.class);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new UserOrderControllerV1(service, checkoutService))
                        .build();
    }

    /** 验证分页返回游标结果. */
    @Test
    void testPageReturnsCursorResult() throws Exception {
        when(service.pageVo(any(OrderPageDto.class)))
                .thenReturn(new CursorPageVo<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc
                .perform(get("/api/v1/user/orders").header("X-User-Id", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
    }

    /** 验证分页使用请求头覆盖客户端用户 ID. */
    @Test
    void testPageOverridesClientUserId() throws Exception {
        when(service.pageVo(any(OrderPageDto.class)))
                .thenReturn(new CursorPageVo<>(java.util.Collections.emptyList(), null, false, 20));

        mockMvc
                .perform(get("/api/v1/user/orders").header("X-User-Id", 7L).param("userId", "999"))
                .andExpect(status().isOk());

        ArgumentCaptor<OrderPageDto> captor = ArgumentCaptor.forClass(OrderPageDto.class);
        verify(service).pageVo(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
    }

    /** 验证状态计数返回计数. */
    @Test
    void testStatusCountsReturnsCounts() throws Exception {
        com.yirancrazy.minimall.order.vo.OrderStatusCountsVo vo =
                new com.yirancrazy.minimall.order.vo.OrderStatusCountsVo();
        vo.setPendingCount(2);
        vo.setPaidCount(1);
        vo.setShippedCount(3);
        vo.setCompletedCount(4);
        when(service.countStatusByUser(7L)).thenReturn(vo);

        mockMvc
                .perform(get("/api/v1/user/orders/status-counts").header("X-User-Id", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.pendingCount").value(2))
                .andExpect(jsonPath("$.data.paidCount").value(1))
                .andExpect(jsonPath("$.data.shippedCount").value(3))
                .andExpect(jsonPath("$.data.completedCount").value(4));
        verify(service).countStatusByUser(7L);
    }

    /** 验证结算返回已创建. */
    @Test
    void testCheckoutReturnsCreated() throws Exception {
        when(checkoutService.checkout(anyLong(), any())).thenReturn(100L);
        OrderCheckoutDto dto = new OrderCheckoutDto();
        dto.setCheckoutSource(CheckoutSourceEnum.BUY_NOW);
        dto.setItems(java.util.List.of(new OrderCheckoutItemDto(1L, 2)));
        mockMvc
                .perform(
                        post("/api/v1/user/orders/checkout")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(100));
        verify(checkoutService).checkout(anyLong(), any());
    }

    /** 验证结算缺少来源时返回 HTTP 400. */
    @Test
    void testCheckoutMissingSourceReturnsBadRequest() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/user/orders/checkout")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cartItemIds\":[9]}"))
                .andExpect(status().isBadRequest());
        verify(checkoutService, never()).checkout(anyLong(), any());
    }

    /** 验证创建使用请求头返回已创建. */
    @Test
    void testCreateWithHeaderReturnsCreated() throws Exception {
        when(service.create(anyLong(), anyLong(), anyInt(), any())).thenReturn(100L);
        OrderCreateDto dto = new OrderCreateDto(100L, 2, null);
        mockMvc
                .perform(
                        post("/api/v1/user/orders")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(100));
        verify(service).create(eq(1L), eq(100L), eq(2), any());
    }

    /** 验证支付端点被移除. */
    @Test
    void testPayEndpointRemoved() throws Exception {
        mockMvc
                .perform(post("/api/v1/user/orders/99/pay").header("X-User-Id", 1L))
                .andExpect(status().isNotFound());
    }

    /** 验证状态返回整数. */
    @Test
    void testStatusReturnsInteger() throws Exception {
        when(service.getStatus(99L, 1L)).thenReturn(2);
        mockMvc
                .perform(get("/api/v1/user/orders/99").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }

    /** 验证详情返回视图对象. */
    @Test
    void testDetailReturnsVo() throws Exception {
        OrderVo vo = new OrderVo();
        vo.setId(99L);
        vo.setUserId(1L);
        vo.setAmount("10");
        vo.setStatus("PAID");
        when(service.detailVo(99L, 1L)).thenReturn(vo);
        mockMvc
                .perform(get("/api/v1/user/orders/99/detail").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(99));
    }

    /** 验证物流返回节点. */
    @Test
    void testLogisticsReturnsNodes() throws Exception {
        when(service.queryLogistics(99L, 1L))
                .thenReturn(java.util.List.of(new com.yirancrazy.minimall.order.vo.OrderLogisticsVo()));
        mockMvc
                .perform(get("/api/v1/user/orders/99/logistics").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(service).queryLogistics(99L, 1L);
    }

    /** 验证取消使用请求头调用服务. */
    @Test
    void testCancelWithHeaderInvokesService() throws Exception {
        mockMvc
                .perform(post("/api/v1/user/orders/99/cancel").header("X-User-Id", 1L))
                .andExpect(status().isOk());
        verify(service).cancel(99L, 1L);
    }

    /** 验证退款使用请求头调用服务. */
    @Test
    void testRefundWithHeaderInvokesService() throws Exception {
        mockMvc
                .perform(post("/api/v1/user/orders/99/refund").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(service).refund(99L, 1L);
    }
}
