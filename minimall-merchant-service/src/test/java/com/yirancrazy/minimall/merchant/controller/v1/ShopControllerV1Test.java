package com.yirancrazy.minimall.merchant.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDto;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDto;
import com.yirancrazy.minimall.merchant.service.ShopService;
import com.yirancrazy.minimall.merchant.vo.ShopVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopControllerV1 HTTP 契约测试；使用 ShopService mock 和 standalone MockMvc 覆盖店铺查询、创建、更新和删除的响应及服务委派。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class ShopControllerV1Test {

    private static final String X_MERCHANT_ID = "X-Merchant-Id";

    private MockMvc mockMvc;
    private ShopService shopService;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 初始化店铺服务 mock 和仅加载 ShopControllerV1 的 MockMvc。
     */
    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ShopControllerV1(shopService)).build();
    }

    /** 验证按 ID 查询店铺；前置服务为商家 1、店铺 99 返回视图；执行 GET；预期 00000 且响应含店铺 ID 和名称. */
    @Test
    void testGetReturnsShop() throws Exception {
        ShopVo vo = new ShopVo();
        vo.setId(99L);
        vo.setMerchantId(1L);
        vo.setShopName("薄荷商城");
        vo.setLicenseNo("ABC123456789012");
        vo.setStatus(com.yirancrazy.minimall.merchant.constant.ShopStatusEnum.ACTIVE.intCode());
        when(shopService.getVo(1L, 99L)).thenReturn(vo);
        mockMvc
                .perform(get("/api/v1/merchant/shops/99").header(X_MERCHANT_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.shopName").value("薄荷商城"));
    }

    /** 验证创建店铺返回 ID；前置请求体合法且服务返回 100；执行 POST；预期 00000、data=100 且商家身份正确透传. */
    @Test
    void testCreateReturnsId() throws Exception {
        when(shopService.create(eq(1L), any(ShopCreateDto.class))).thenReturn(100L);
        ShopCreateDto dto = new ShopCreateDto();
        dto.setShopName("新店");
        dto.setLicenseNo("ABC123456789012");
        mockMvc
                .perform(
                        post("/api/v1/merchant/shops")
                                .header(X_MERCHANT_ID, "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
        verify(shopService).create(eq(1L), any(ShopCreateDto.class));
    }

    /** 验证更新店铺返回成功标志；前置更新体合法且服务返回 true；执行 PUT；预期 00000、data=true 且服务被调用. */
    @Test
    void testUpdateReturnsBoolean() throws Exception {
        when(shopService.update(eq(1L), anyLong(), any(ShopUpdateDto.class))).thenReturn(true);
        ShopUpdateDto dto = new ShopUpdateDto();
        dto.setShopName("改名后");
        dto.setLicenseNo("XYZ987654321098");
        dto.setStatus("ACTIVE");
        mockMvc
                .perform(
                        put("/api/v1/merchant/shops/99")
                                .header(X_MERCHANT_ID, "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(shopService).update(eq(1L), anyLong(), any(ShopUpdateDto.class));
    }

    /** 验证删除店铺返回结果；前置服务对商家 1、店铺 99 返回 true；执行 DELETE；预期 00000、data=true 且 ID 透传正确. */
    @Test
    void testDeleteReturnsBoolean() throws Exception {
        when(shopService.delete(1L, 99L)).thenReturn(true);
        mockMvc
                .perform(delete("/api/v1/merchant/shops/99").header(X_MERCHANT_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(shopService).delete(1L, 99L);
    }
}
