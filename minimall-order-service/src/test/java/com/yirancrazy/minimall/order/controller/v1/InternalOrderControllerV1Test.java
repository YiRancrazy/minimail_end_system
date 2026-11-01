package com.yirancrazy.minimall.order.controller.v1;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.api.dto.order.OrderCommentEligibilityDto;
import com.yirancrazy.minimall.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证内部订单控制器的评论资格接口映射和响应。
 *   业务边界：使用 standalone MockMvc 与 OrderService Mockito 桩，不启动完整 Spring 上下文。
 *   使用场景：覆盖内部评论资格快照的符合与不符合两种响应。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class InternalOrderControllerV1Test {

    private MockMvc mockMvc;
    private OrderService service;

    /**
     * 初始化当前测试类的共享夹具。
     *
     * <p>按测试类型创建 Mockito 桩、事务模板替身或 MockMvc，并为成功路径设置宽松默认值。
     */
    @BeforeEach
    void setUp() {
        service = mock(OrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalOrderControllerV1(service)).build();
    }

    /** 验证评论资格返回快照. */
    @Test
    void testCommentEligibilityReturnsSnapshot() throws Exception {
        when(service.commentEligibility(eq("OD10001"), anyLong(), anyLong()))
                .thenReturn(new OrderCommentEligibilityDto(true, 99L, 7L, 11L, 4));

        mockMvc
                .perform(
                        get("/internal/order/comment-eligibility")
                                .param("orderNo", "OD10001")
                                .param("userId", "7")
                                .param("spuId", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.merchantId").value(11));

        verify(service).commentEligibility("OD10001", 7L, 500L);
    }

    /** 验证评论资格返回不符合条件. */
    @Test
    void testCommentEligibilityReturnsNotEligible() throws Exception {
        when(service.commentEligibility(eq("OD10002"), anyLong(), anyLong()))
                .thenReturn(new OrderCommentEligibilityDto(false, 100L, 8L, null, 1));

        mockMvc
                .perform(
                        get("/internal/order/comment-eligibility")
                                .param("orderNo", "OD10002")
                                .param("userId", "8")
                                .param("spuId", "600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.eligible").value(false));
    }
}
