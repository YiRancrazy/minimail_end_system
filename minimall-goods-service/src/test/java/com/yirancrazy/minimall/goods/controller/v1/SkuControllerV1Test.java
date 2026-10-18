package com.yirancrazy.minimall.goods.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
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
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.vo.SkuVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class SkuControllerV1Test {

    private MockMvc mockMvc;
    private SkuService skuService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        skuService = mock(SkuService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkuControllerV1(skuService)).build();
    }


    @Test
    void get_returns_sku() throws Exception {
        SkuVO vo = new SkuVO();
        vo.setId(99L);
        vo.setSpuId(1L);
        vo.setSkuName("薄荷洗发水");
        vo.setPrice(java.math.BigDecimal.TEN);
        vo.setStock(100);
        when(skuService.getVO(99L, 7L)).thenReturn(vo);
        mockMvc.perform(get("/api/v1/merchant/goods/skus/99")
                .header("X-Merchant-Id", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.skuName").value("薄荷洗发水"));
        verify(skuService).getVO(99L, 7L);
    }


    @Test
    void create_returns_id() throws Exception {
        when(skuService.create(eq(7L), any(SkuCreateDTO.class))).thenReturn(100L);
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("薄荷洗发水");
        dto.setPrice(java.math.BigDecimal.TEN);
        dto.setStock(100);
        mockMvc.perform(post("/api/v1/merchant/goods/skus")
                .header("X-Merchant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
        verify(skuService).create(eq(7L), any(SkuCreateDTO.class));
    }


    @Test
    void page_returns_cursor_result() throws Exception {
        when(skuService.pageVO(any(com.yirancrazy.minimall.goods.dto.SkuPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/goods/skus")
                .header("X-Merchant-Id", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(skuService).pageVO(any(com.yirancrazy.minimall.goods.dto.SkuPageDTO.class));
    }


    @Test
    void update_returns_boolean() throws Exception {
        when(skuService.update(eq(99L), eq(7L), any(SkuUpdateDTO.class))).thenReturn(true);
        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("改名后");
        dto.setPrice(java.math.BigDecimal.ONE);
        dto.setStock(50);
        mockMvc.perform(put("/api/v1/merchant/goods/skus/99")
                .header("X-Merchant-Id", "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(skuService).update(eq(99L), eq(7L), any(SkuUpdateDTO.class));
    }


    @Test
    void delete_returns_boolean() throws Exception {
        when(skuService.delete(99L, 7L)).thenReturn(true);
        mockMvc.perform(delete("/api/v1/merchant/goods/skus/99")
                .header("X-Merchant-Id", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(skuService).delete(99L, 7L);
    }
}
