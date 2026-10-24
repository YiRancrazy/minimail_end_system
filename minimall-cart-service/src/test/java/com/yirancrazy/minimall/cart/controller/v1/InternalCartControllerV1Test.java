package com.yirancrazy.minimall.cart.controller.v1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDTO;
import com.yirancrazy.minimall.cart.service.CartService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车内部结算接口测试。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
class InternalCartControllerV1Test {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CartService cartService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new InternalCartControllerV1(cartService))
            .build();
    }

    @Test
    void checkoutSnapshot_returnsAuthoritativeItems() throws Exception {
        CartCheckoutQueryDTO dto = new CartCheckoutQueryDTO(7L, List.of(9001L));
        when(cartService.getCheckoutItems(7L, List.of(9001L)))
            .thenReturn(List.of(new CartCheckoutItemDTO(9001L, 100L, 2)));

        mockMvc.perform(post("/internal/cart/checkout-snapshot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].cartItemId").value(9001))
            .andExpect(jsonPath("$.data[0].skuId").value(100))
            .andExpect(jsonPath("$.data[0].quantity").value(2));
    }

    @Test
    void checkoutCleanup_delegatesToService() throws Exception {
        CartCheckoutCleanupDTO dto = new CartCheckoutCleanupDTO(
            7L, List.of(new CartCheckoutItemDTO(9001L, 100L, 2)));

        mockMvc.perform(post("/internal/cart/checkout-cleanup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));

        verify(cartService).cleanupCheckout(any(), any());
    }
}
