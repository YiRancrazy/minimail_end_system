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
import com.yirancrazy.minimall.merchant.service.ShopService;
import com.yirancrazy.minimall.merchant.vo.ShopVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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


    @Test
    void get_returns_shop() throws Exception {
        ShopVO vo = new ShopVO();
        vo.setId(99L);
        vo.setMerchantId(1L);
        vo.setShopName("薄荷商城");
        vo.setLicenseNo("ABC123456789012");
        vo.setStatus(com.yirancrazy.minimall.merchant.constant.ShopStatusEnum.ACTIVE.intCode());
        when(shopService.getVO(1L, 99L)).thenReturn(vo);
        mockMvc.perform(get("/api/v1/merchant/shops/99")
                .header(X_MERCHANT_ID, "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.shopName").value("薄荷商城"));
    }


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
