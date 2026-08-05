package com.yirancrazy.minimall.goods.controller.v1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.service.SkuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuControllerV1 MockMvc 单元测试，验证 SKU CRUD 端点的 HTTP 路由、参数绑定与 Result 包装。
 * @Version: 1.0
 * @DateTime: 2026/08/05
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

    /**
     * 验证 GET /{id} 返回 SKU 详情。
     */
    @Test
    void get_returns_sku() throws Exception {
        SkuPO po = new SkuPO();
        po.setId(99L);
        po.setSpuId(1L);
        po.setSkuName("薄荷洗发水");
        po.setPrice(java.math.BigDecimal.TEN);
        po.setStock(100);
        when(skuService.getById(99L)).thenReturn(po);
        mockMvc.perform(get("/api/v1/merchant/goods/skus/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.id").value(99))
            .andExpect(jsonPath("$.data.skuName").value("薄荷洗发水"));
        verify(skuService).getById(99L);
    }

    /**
     * 验证 POST / 创建 SKU 并返回 ID。
     */
    @Test
    void create_returns_id() throws Exception {
        when(skuService.create(any(SkuCreateDTO.class))).thenReturn(100L);
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("薄荷洗发水");
        dto.setPrice(java.math.BigDecimal.TEN);
        dto.setStock(100);
        mockMvc.perform(post("/api/v1/merchant/goods/skus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(100));
        verify(skuService).create(any(SkuCreateDTO.class));
    }

    /**
     * 验证 GET / 返回游标分页结果。
     */
    @Test
    void page_returns_cursor_result() throws Exception {
        when(skuService.page(any(com.yirancrazy.minimall.goods.dto.SkuPageDTO.class)))
            .thenReturn(new CursorPageVO<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc.perform(get("/api/v1/merchant/goods/skus"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));
        verify(skuService).page(any(com.yirancrazy.minimall.goods.dto.SkuPageDTO.class));
    }

    /**
     * 验证 PUT /{id} 更新 SKU 并返回成功。
     */
    @Test
    void update_returns_boolean() throws Exception {
        when(skuService.update(anyLong(), any(SkuUpdateDTO.class))).thenReturn(true);
        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("改名后");
        dto.setPrice(java.math.BigDecimal.ONE);
        dto.setStock(50);
        mockMvc.perform(put("/api/v1/merchant/goods/skus/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(skuService).update(anyLong(), any(SkuUpdateDTO.class));
    }

    /**
     * 验证 DELETE /{id} 删除 SKU 并返回成功。
     */
    @Test
    void delete_returns_boolean() throws Exception {
        when(skuService.delete(99L)).thenReturn(true);
        mockMvc.perform(delete("/api/v1/merchant/goods/skus/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").value(true));
        verify(skuService).delete(99L);
    }
}
