package com.yirancrazy.minimall.cart.controller.v1;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.service.CartService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class CartControllerV1Test {

    private MockMvc mockMvc;
    private CartService cartService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartControllerV1(cartService)).build();
    }


    @Test
    void list_returns_cart_items() throws Exception {
        when(cartService.listByUser(anyLong()))
            .thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/api/v1/user/cart").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(cartService).listByUser(1L);
    }


    @Test
    void add_returns_id() throws Exception {
        when(cartService.add(anyLong(), any(CartItemAddDTO.class))).thenReturn(100L);
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(99L);
        dto.setQuantity(2);
        dto.setSelected(1);
        mockMvc.perform(post("/api/v1/user/cart")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
        verify(cartService).add(eq(1L), any(CartItemAddDTO.class));
    }


    @Test
    void update_returns_boolean() throws Exception {
        when(cartService.update(anyLong(), anyLong(), any(CartUpdateDTO.class))).thenReturn(true);
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(3);
        dto.setIsSelected(true);
        mockMvc.perform(patch("/api/v1/user/cart/99")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(cartService).update(eq(99L), eq(1L), any(CartUpdateDTO.class));
    }


    @Test
    void delete_invokes_service_with_user() throws Exception {
        when(cartService.delete(anyLong(), anyLong())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/user/cart/99").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(cartService).delete(99L, 1L);
    }


    @Test
    void add_rejects_quantity_over_max() throws Exception {
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setSkuId(99L);
        dto.setQuantity(1000);
        mockMvc.perform(post("/api/v1/user/cart")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
        verify(cartService, never()).add(any(), any());
    }


    @Test
    void update_rejects_quantity_over_max() throws Exception {
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(1000);
        mockMvc.perform(patch("/api/v1/user/cart/99")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
        verify(cartService, never()).update(any(), any(), any());
    }


    @Test
    void clear_returns_boolean() throws Exception {
        when(cartService.clear(anyLong())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/user/cart").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(cartService).clear(1L);
    }


    @Test
    void move_to_favorite_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/cart/move-to-favorite")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemIds\":[99,100]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(cartService).moveToFavorite(1L, List.of(99L, 100L));
    }


    @Test
    void move_to_favorite_rejects_empty_item_ids() throws Exception {
        mockMvc.perform(post("/api/v1/user/cart/move-to-favorite")
                .header("X-User-Id", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemIds\":[]}"))
            .andExpect(status().isBadRequest());
        verify(cartService, never()).moveToFavorite(any(), any());
    }
}
