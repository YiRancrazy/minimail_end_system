package com.yirancrazy.minimall.merchant.controller.v1;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.service.ShopService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopControllerV1 MockMvc 单元测试，验证店铺 CRUD 端点的 HTTP 路由、参数绑定与 Result 包装。
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
class ShopControllerV1Test {

    private static final String X_MERCHANT_ID = "X-Merchant-Id";

    private MockMvc mockMvc;
    private ShopService shopService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        shopService = mock(ShopService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ShopControllerV1(shopService)).build();
    }

    /**
     * 验证 GET /api/v1/merchant/shops/{id} 携带商家头返回店铺详情。
     */
    @Test
    void get_returns_shop() throws Exception {
        ShopPO po = new ShopPO();
        po.setId(99L);
        po.setMerchantId(1L);
        po.setShopName("薄荷商城");
        po.setLicenseNo("ABC123456789012");
        po.setStatus(com.yirancrazy.minimall.merchant.constant.ShopStatusEnum.ACTIVE.intCode());
        when(shopService.getById(1L, 99L)).thenReturn(po);
        mockMvc.perform(get("/api/v1/merchant/shops/99")
                .header(X_MERCHANT_ID, "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.shopName").value("薄荷商城"));
    }

    /**
     * 验证 POST /api/v1/merchant/shops 携带商家头创建店铺并返回店铺 ID。
     */
    @Test
    void create_returns_id() throws Exception {
        when(shopService.create(eq(1L), any(ShopCreateDTO.class))).thenReturn(100L);
        ShopCreateDTO dto = new ShopCreateDTO();
        dto.setShopName("新店");
        dto.setLicenseNo("ABC123456789012");
        mockMvc.perform(post("/api/v1/merchant/shops")
                .header(X_MERCHANT_ID, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
        verify(shopService).create(eq(1L), any(ShopCreateDTO.class));
    }

    /**
     * 验证 PUT /api/v1/merchant/shops/{id} 携带商家头更新店铺信息并返回成功。
     */
    @Test
    void update_returns_boolean() throws Exception {
        when(shopService.update(eq(1L), anyLong(), any(ShopUpdateDTO.class))).thenReturn(true);
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("改名后");
        dto.setLicenseNo("XYZ987654321098");
        dto.setStatus("ACTIVE");
        mockMvc.perform(put("/api/v1/merchant/shops/99")
                .header(X_MERCHANT_ID, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(shopService).update(eq(1L), anyLong(), any(ShopUpdateDTO.class));
    }

    /**
     * 验证 DELETE /api/v1/merchant/shops/{id} 携带商家头删除店铺并返回成功。
     */
    @Test
    void delete_returns_boolean() throws Exception {
        when(shopService.delete(1L, 99L)).thenReturn(true);
        mockMvc.perform(delete("/api/v1/merchant/shops/99")
                .header(X_MERCHANT_ID, "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(shopService).delete(1L, 99L);
    }
}
