package com.yirancrazy.minimall.cart.controller.v1;

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
import com.yirancrazy.minimall.cart.dto.CartItemAddDto;
import com.yirancrazy.minimall.cart.dto.CartUpdateDto;
import com.yirancrazy.minimall.cart.service.CartService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 使用独立 MockMvc 和模拟 CartService 测试 CartControllerV1。覆盖 HTTP 路由、
 *     请求头用户标识传递、Result 序列化、服务委托，以及数量和移入收藏载荷的 Jakarta 校验；
 *     测试不加载完整 Spring 上下文。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class CartControllerV1Test {

    private MockMvc mockMvc;
    private CartService cartService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CartControllerV1(cartService)).build();
    }

    /** 验证列表接口传递请求头用户标识，并返回成功码和空购物车数据. */
    @Test
    void testListReturnsCartItems() throws Exception {
        when(cartService.listByUser(anyLong())).thenReturn(java.util.Collections.emptyList());
        mockMvc
                .perform(get("/api/v1/user/cart").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(cartService).listByUser(1L);
    }

    /** 验证添加接口返回服务生成的条目主键，并使用请求头中的用户标识. */
    @Test
    void testAddReturnsId() throws Exception {
        when(cartService.add(anyLong(), any(CartItemAddDto.class))).thenReturn(100L);
        CartItemAddDto dto = new CartItemAddDto();
        dto.setSkuId(99L);
        dto.setQuantity(2);
        dto.setSelected(1);
        mockMvc
                .perform(
                        post("/api/v1/user/cart")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
        verify(cartService).add(eq(1L), any(CartItemAddDto.class));
    }

    /** 验证更新接口传递路径条目主键和请求头用户标识，并返回服务布尔结果. */
    @Test
    void testUpdateReturnsBoolean() throws Exception {
        when(cartService.update(anyLong(), anyLong(), any(CartUpdateDto.class))).thenReturn(true);
        CartUpdateDto dto = new CartUpdateDto();
        dto.setQuantity(3);
        dto.setIsSelected(true);
        mockMvc
                .perform(
                        patch("/api/v1/user/cart/99")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(cartService).update(eq(99L), eq(1L), any(CartUpdateDto.class));
    }

    /** 验证删除接口组合路径条目主键与请求头用户标识调用服务，并返回删除结果. */
    @Test
    void testDeleteInvokesServiceWithUser() throws Exception {
        when(cartService.delete(anyLong(), anyLong())).thenReturn(true);
        mockMvc
                .perform(delete("/api/v1/user/cart/99").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(cartService).delete(99L, 1L);
    }

    /** 验证添加接口拒绝数量 1000，返回 400 且不调用服务. */
    @Test
    void testAddRejectsQuantityOverMax() throws Exception {
        CartItemAddDto dto = new CartItemAddDto();
        dto.setSkuId(99L);
        dto.setQuantity(1000);
        mockMvc
                .perform(
                        post("/api/v1/user/cart")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        verify(cartService, never()).add(any(), any());
    }

    /** 验证更新接口拒绝数量 1000，返回 400 且不调用服务. */
    @Test
    void testUpdateRejectsQuantityOverMax() throws Exception {
        CartUpdateDto dto = new CartUpdateDto();
        dto.setQuantity(1000);
        mockMvc
                .perform(
                        patch("/api/v1/user/cart/99")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        verify(cartService, never()).update(any(), any(), any());
    }

    /** 验证清空接口使用请求头用户标识调用服务，并返回成功布尔结果. */
    @Test
    void testClearReturnsBoolean() throws Exception {
        when(cartService.clear(anyLong())).thenReturn(true);
        mockMvc
                .perform(delete("/api/v1/user/cart").header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(cartService).clear(1L);
    }

    /** 验证移入收藏接口原序传递条目主键列表，并返回统一成功响应. */
    @Test
    void testMoveToFavoriteInvokesService() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/user/cart/move-to-favorite")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"itemIds\":[99,100]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(cartService).moveToFavorite(1L, List.of(99L, 100L));
    }

    /** 验证移入收藏接口拒绝空条目主键列表，返回 400 且不调用服务. */
    @Test
    void testMoveToFavoriteRejectsEmptyItemIds() throws Exception {
        mockMvc
                .perform(
                        post("/api/v1/user/cart/move-to-favorite")
                                .header("X-User-Id", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"itemIds\":[]}"))
                .andExpect(status().isBadRequest());
        verify(cartService, never()).moveToFavorite(any(), any());
    }
}
