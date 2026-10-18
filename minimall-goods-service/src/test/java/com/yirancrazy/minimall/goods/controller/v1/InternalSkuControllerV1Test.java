package com.yirancrazy.minimall.goods.controller.v1;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.service.SkuService;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalSkuControllerV1Test 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
class InternalSkuControllerV1Test {

    private MockMvc mockMvc;
    private SkuService skuService;

    @BeforeEach
    void setUp() {
        skuService = mock(SkuService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalSkuControllerV1(skuService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }


    @Test
    void snapshot_returns_snapshot_when_spu_on_sale() throws Exception {
        when(skuService.getSnapshot(99L)).thenReturn(new SkuSnapshotDTO(
            99L, 1L, "薄荷洗发水", new BigDecimal("19.90"), 100, 7L));

        mockMvc.perform(get("/internal/goods/sku/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data.skuId").value(99))
            .andExpect(jsonPath("$.data.spuId").value(1))
            .andExpect(jsonPath("$.data.skuName").value("薄荷洗发水"))
            .andExpect(jsonPath("$.data.price").value(19.90))
            .andExpect(jsonPath("$.data.stock").value(100))
            .andExpect(jsonPath("$.data.merchantId").value(7));
    }


    @Test
    void snapshot_rejects_when_spu_not_on_sale() throws Exception {
        when(skuService.getSnapshot(99L)).thenThrow(new BizException(SpuCodeEnum.SPU_NOT_ON_SALE));

        mockMvc.perform(get("/internal/goods/sku/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(SpuCodeEnum.SPU_NOT_ON_SALE.getCode()))
            .andExpect(jsonPath("$.message").value(SpuCodeEnum.SPU_NOT_ON_SALE.getMessage()));
    }


    @Test
    void snapshot_propagates_spu_not_found() throws Exception {
        when(skuService.getSnapshot(99L)).thenThrow(new BizException(SpuCodeEnum.SPU_NOT_FOUND));

        mockMvc.perform(get("/internal/goods/sku/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(SpuCodeEnum.SPU_NOT_FOUND.getCode()));
    }


    @Test
    void batchSnapshot_returns_snapshot_map() throws Exception {
        when(skuService.listSnapshots(List.of(99L))).thenReturn(Map.of(
            99L, new SkuSnapshotDTO(99L, 1L, "薄荷洗发水", new BigDecimal("19.90"), 100, 7L)));

        mockMvc.perform(post("/internal/goods/sku/batch-snapshot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[99]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data['99'].skuId").value(99))
            .andExpect(jsonPath("$.data['99'].spuId").value(1))
            .andExpect(jsonPath("$.data['99'].skuName").value("薄荷洗发水"))
            .andExpect(jsonPath("$.data['99'].merchantId").value(7));
    }


    @Test
    void batchSnapshot_empty_ids_returns_empty_map() throws Exception {
        when(skuService.listSnapshots(List.of())).thenReturn(Map.of());

        mockMvc.perform(post("/internal/goods/sku/batch-snapshot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").isEmpty());
    }
}
