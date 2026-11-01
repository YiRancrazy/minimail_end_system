package com.yirancrazy.minimall.goods.controller.v1;

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
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.goods.dto.SkuCreateDto;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDto;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.vo.SkuVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证商家 SKU 控制器对请求头、DTO、服务调用和统一响应的转发。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class SkuControllerV1Test {

    private MockMvc mockMvc;
    private SkuService skuService;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        skuService = mock(SkuService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SkuControllerV1(skuService)).build();
    }

    /** * 验证商家读取 SKU 时透传商家头和路径主键并返回统一响应. */
    @Test
    void testGetReturnsSku() throws Exception {
        SkuVo vo = new SkuVo();
        vo.setId(99L);
        vo.setSpuId(1L);
        vo.setSkuName("薄荷洗发水");
        vo.setPrice(java.math.BigDecimal.TEN);
        vo.setStock(100);
        when(skuService.getVo(99L, 7L)).thenReturn(vo);
        mockMvc
                .perform(get("/api/v1/merchant/goods/skus/99").header("X-Merchant-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(99))
                .andExpect(jsonPath("$.data.skuName").value("薄荷洗发水"));
        verify(skuService).getVo(99L, 7L);
    }

    /** * 验证创建 SKU 时校验请求体并返回新建主键. */
    @Test
    void testCreateReturnsId() throws Exception {
        when(skuService.create(eq(7L), any(SkuCreateDto.class))).thenReturn(100L);
        SkuCreateDto dto = new SkuCreateDto();
        dto.setSpuId(1L);
        dto.setSkuName("薄荷洗发水");
        dto.setPrice(java.math.BigDecimal.TEN);
        dto.setStock(100);
        mockMvc
                .perform(
                        post("/api/v1/merchant/goods/skus")
                                .header("X-Merchant-Id", "7")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(100));
        verify(skuService).create(eq(7L), any(SkuCreateDto.class));
    }

    /** * 验证 SKU 分页接口将商家头写入查询条件并返回游标结果. */
    @Test
    void testPageReturnsCursorResult() throws Exception {
        when(skuService.pageVo(any(com.yirancrazy.minimall.goods.dto.SkuPageDto.class)))
                .thenReturn(new CursorPageVo<>(java.util.Collections.emptyList(), null, false, 20));
        mockMvc
                .perform(get("/api/v1/merchant/goods/skus").header("X-Merchant-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"));
        verify(skuService).pageVo(any(com.yirancrazy.minimall.goods.dto.SkuPageDto.class));
    }

    /** * 验证更新 SKU 时透传商家、主键和增量字段并返回操作结果. */
    @Test
    void testUpdateReturnsBoolean() throws Exception {
        when(skuService.update(eq(99L), eq(7L), any(SkuUpdateDto.class))).thenReturn(true);
        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("改名后");
        dto.setPrice(java.math.BigDecimal.ONE);
        dto.setStock(50);
        mockMvc
                .perform(
                        put("/api/v1/merchant/goods/skus/99")
                                .header("X-Merchant-Id", "7")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(skuService).update(eq(99L), eq(7L), any(SkuUpdateDto.class));
    }

    /** * 验证删除 SKU 时透传商家和主键并返回删除结果. */
    @Test
    void testDeleteReturnsBoolean() throws Exception {
        when(skuService.delete(99L, 7L)).thenReturn(true);
        mockMvc
                .perform(delete("/api/v1/merchant/goods/skus/99").header("X-Merchant-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data").value(true));
        verify(skuService).delete(99L, 7L);
    }
}
