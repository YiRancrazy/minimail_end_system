package com.yirancrazy.minimall.cart.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.cart.service.CartService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用独立 MockMvc 和模拟 CartService 测试 InternalCartControllerV1。场景验证结算
 *     快照序列化和结算清理服务委托；独立测试配置不安装 InternalAuthFilter，因此令牌校验由其他
 *     测试覆盖。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class InternalCartControllerV1Test {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CartService cartService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalCartControllerV1(cartService)).build();
    }

    /** 验证结算快照接口原样返回服务提供的条目主键、SKU 主键和数量. */
    @Test
    void testCheckoutSnapshotReturnsAuthoritativeItems() throws Exception {
        CartCheckoutQueryDto dto = new CartCheckoutQueryDto(7L, List.of(9001L));
        when(cartService.getCheckoutItems(7L, List.of(9001L)))
                .thenReturn(List.of(new CartCheckoutItemDto(9001L, 100L, 2)));

        mockMvc
                .perform(
                        post("/internal/cart/checkout-snapshot")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cartItemId").value(9001))
                .andExpect(jsonPath("$.data[0].skuId").value(100))
                .andExpect(jsonPath("$.data[0].quantity").value(2));
    }

    /** 验证结算清理接口委派服务后返回成功码，不校验具体参数内容. */
    @Test
    void testCheckoutCleanupDelegatesToService() throws Exception {
        CartCheckoutCleanupDto dto =
                new CartCheckoutCleanupDto(7L, List.of(new CartCheckoutItemDto(9001L, 100L, 2)));

        mockMvc
                .perform(
                        post("/internal/cart/checkout-cleanup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));

        verify(cartService).cleanupCheckout(any(), any());
    }
}
