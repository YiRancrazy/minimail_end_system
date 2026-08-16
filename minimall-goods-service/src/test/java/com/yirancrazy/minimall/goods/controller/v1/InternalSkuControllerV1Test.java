package com.yirancrazy.minimall.goods.controller.v1;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.exception.GlobalExceptionHandler;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.service.SkuService;
import com.yirancrazy.minimall.goods.service.SpuService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: InternalSkuControllerV1 MockMvc 单元测试，验证内部 SKU 快照接口的在售校验与快照返回。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
class InternalSkuControllerV1Test {

    private MockMvc mockMvc;
    private SkuService skuService;
    private SpuService spuService;

    @BeforeEach
    void setUp() {
        skuService = mock(SkuService.class);
        spuService = mock(SpuService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalSkuControllerV1(skuService, spuService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    /**
     * 验证所属 SPU 在售时返回 SKU 快照，含商家 ID。
     */
    @Test
    void snapshot_returns_snapshot_when_spu_on_sale() throws Exception {
        SkuPO sku = new SkuPO();
        sku.setId(99L);
        sku.setSpuId(1L);
        sku.setSkuName("薄荷洗发水");
        sku.setPrice(new BigDecimal("19.90"));
        sku.setStock(100);
        when(skuService.getById(99L)).thenReturn(sku);
        SpuPO spu = new SpuPO();
        spu.setId(1L);
        spu.setMerchantId(7L);
        spu.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuService.getById(1L)).thenReturn(spu);

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

    /**
     * 验证所属 SPU 处于任一非在售状态（草稿/待审/下架/驳回）时拒绝返回快照，防止未过审商品被下单。
     */
    @Test
    void snapshot_rejects_when_spu_not_on_sale() throws Exception {
        SkuPO sku = new SkuPO();
        sku.setId(99L);
        sku.setSpuId(1L);
        sku.setSkuName("薄荷洗发水");
        sku.setPrice(new BigDecimal("19.90"));
        sku.setStock(100);
        when(skuService.getById(99L)).thenReturn(sku);
        SpuPO spu = new SpuPO();
        spu.setId(1L);
        spu.setMerchantId(7L);
        when(spuService.getById(1L)).thenReturn(spu);
        for (SpuStatusEnum status : List.of(SpuStatusEnum.DRAFT, SpuStatusEnum.PENDING_AUDIT,
            SpuStatusEnum.OFF_SHELF, SpuStatusEnum.REJECTED)) {
            spu.setStatus(status.statusValue());
            mockMvc.perform(get("/internal/goods/sku/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SpuCodeEnum.SPU_NOT_ON_SALE.getCode()))
                .andExpect(jsonPath("$.message").value(SpuCodeEnum.SPU_NOT_ON_SALE.getMessage()));
        }
    }

    /**
     * 验证所属 SPU 不存在时透传 SPU_NOT_FOUND 业务错误。
     */
    @Test
    void snapshot_propagates_spu_not_found() throws Exception {
        SkuPO sku = new SkuPO();
        sku.setId(99L);
        sku.setSpuId(1L);
        when(skuService.getById(99L)).thenReturn(sku);
        when(spuService.getById(1L)).thenThrow(new BizException(SpuCodeEnum.SPU_NOT_FOUND));

        mockMvc.perform(get("/internal/goods/sku/99"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(SpuCodeEnum.SPU_NOT_FOUND.getCode()));
    }
}
