package com.yirancrazy.minimall.cart.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
 * @Description: CartControllerV1 MockMvc 单元测试，验证购物车 CRUD、清空与移入收藏夹等端点的 HTTP 路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/05
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

    /**
     * 验证 GET /（带 X-User-Id 头）返回购物车条目列表。
     */
    @Test
    void list_returns_cart_items() throws Exception {
        when(cartService.listByUser(anyLong()))
            .thenReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/api/v1/user/cart").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(cartService).listByUser(1L);
    }

    /**
     * 验证 POST /（带 X-User-Id 头）添加购物车项并返回 ID。
     */
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

    /**
     * 验证 PATCH /{id} 部分更新购物车项并返回成功。
     */
    @Test
    void update_returns_boolean() throws Exception {
        when(cartService.update(anyLong(), any(CartUpdateDTO.class))).thenReturn(true);
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(3);
        dto.setIsSelected(1);
        mockMvc.perform(patch("/api/v1/user/cart/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(cartService).update(anyLong(), any(CartUpdateDTO.class));
    }

    /**
     * 验证 DELETE /（带 X-User-Id 头）清空用户购物车并返回成功。
     */
    @Test
    void clear_returns_boolean() throws Exception {
        when(cartService.clear(anyLong())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/user/cart").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(cartService).clear(1L);
    }

    /**
     * 验证 POST /{skuId}/move-to-favorite 带 X-User-Id 头调用 service 完成购物车项移入收藏夹。
     */
    @Test
    void move_to_favorite_invokes_service() throws Exception {
        mockMvc.perform(post("/api/v1/user/cart/99/move-to-favorite").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(cartService).moveToFavorite(1L, 99L);
    }
}
