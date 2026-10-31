package com.yirancrazy.minimall.goods.controller.v1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.service.SkuService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证内部 SKU 快照接口的正常返回、在售边界和业务异常传播。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class InternalSkuControllerV1Test {

  private MockMvc mockMvc;
  private SkuService skuService;

  @BeforeEach
  void setUp() {
    skuService = mock(SkuService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new InternalSkuControllerV1(skuService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  /** * 验证 SPU 在售时快照接口返回 SKU 价格、库存与商家信息. */
  @Test
  void testSnapshotReturnsSnapshotWhenSpuOnSale() throws Exception {
    when(skuService.getSnapshot(99L))
        .thenReturn(new SkuSnapshotDto(99L, 1L, "薄荷洗发水", new BigDecimal("19.90"), 100, 7L));

    mockMvc
        .perform(get("/internal/goods/sku/99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data.skuId").value(99))
        .andExpect(jsonPath("$.data.spuId").value(1))
        .andExpect(jsonPath("$.data.skuName").value("薄荷洗发水"))
        .andExpect(jsonPath("$.data.price").value(19.90))
        .andExpect(jsonPath("$.data.stock").value(100))
        .andExpect(jsonPath("$.data.merchantId").value(7));
  }

  /** * 验证 SPU 非在售时快照接口返回非在售业务错误. */
  @Test
  void testSnapshotRejectsWhenSpuNotOnSale() throws Exception {
    when(skuService.getSnapshot(99L)).thenThrow(new BizException(SpuCodeEnum.SPU_NOT_ON_SALE));

    mockMvc
        .perform(get("/internal/goods/sku/99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(SpuCodeEnum.SPU_NOT_ON_SALE.getCode()))
        .andExpect(jsonPath("$.message").value(SpuCodeEnum.SPU_NOT_ON_SALE.getMessage()));
  }

  /** * 验证 SPU 不存在时快照接口返回未找到业务错误. */
  @Test
  void testSnapshotPropagatesSpuNotFound() throws Exception {
    when(skuService.getSnapshot(99L)).thenThrow(new BizException(SpuCodeEnum.SPU_NOT_FOUND));

    mockMvc
        .perform(get("/internal/goods/sku/99"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(SpuCodeEnum.SPU_NOT_FOUND.getCode()));
  }

  /** * 验证批量快照接口按 SKU 主键返回完整快照映射. */
  @Test
  void testBatchSnapshotReturnsSnapshotMap() throws Exception {
    when(skuService.listSnapshots(List.of(99L)))
        .thenReturn(
            Map.of(99L, new SkuSnapshotDto(99L, 1L, "薄荷洗发水", new BigDecimal("19.90"), 100, 7L)));

    mockMvc
        .perform(
            post("/internal/goods/sku/batch-snapshot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[99]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data['99'].skuId").value(99))
        .andExpect(jsonPath("$.data['99'].spuId").value(1))
        .andExpect(jsonPath("$.data['99'].skuName").value("薄荷洗发水"))
        .andExpect(jsonPath("$.data['99'].merchantId").value(7));
  }

  /** * 验证批量快照接口收到空主键列表时返回空映射. */
  @Test
  void testBatchSnapshotEmptyIdsReturnsEmptyMap() throws Exception {
    when(skuService.listSnapshots(List.of())).thenReturn(Map.of());

    mockMvc
        .perform(
            post("/internal/goods/sku/batch-snapshot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data").isEmpty());
  }
}
