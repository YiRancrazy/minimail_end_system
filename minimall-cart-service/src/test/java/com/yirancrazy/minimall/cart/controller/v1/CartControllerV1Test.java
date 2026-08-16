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
     * 验证 PATCH /{id}（带 X-User-Id 头）部分更新购物车项并返回成功。
     */
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

    /**
     * 验证 DELETE /{id}（带 X-User-Id 头）删除购物车项并返回成功。
     */
    @Test
    void delete_invokes_service_with_user() throws Exception {
        when(cartService.delete(anyLong(), anyLong())).thenReturn(true);
        mockMvc.perform(delete("/api/v1/user/cart/99").header("X-User-Id", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(cartService).delete(99L, 1L);
    }

    /**
     * 验证 POST / 在 quantity 超过上限 999 时被 @Valid 拦截返回 400。
     */
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

    /**
     * 验证 PATCH /{id} 在 quantity 超过上限 999 时被 @Valid 拦截返回 400。
     */
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
     * 验证 POST /move-to-favorite 带 X-User-Id 头与条目 ID 列表调用 service 完成购物车项移入收藏夹。
     */
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

    /**
     * 验证 POST /move-to-favorite 在 itemIds 为空时被 @Valid 拦截返回 400。
     */
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
