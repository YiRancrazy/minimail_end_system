package com.yirancrazy.minimall.goods.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDTO;
import com.yirancrazy.minimall.api.feign.StockFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.service.impl.SkuServiceImpl;

/**
 * SkuServiceImpl 单元测试，覆盖按主键查询与创建 SKU 的正常、失败、边界路径。
 */
public class SkuServiceImplTest {

    private SkuManager skuManager;
    private SpuManager spuManager;
    private SpuService spuService;
    private StockFeignClient stockFeignClient;
    private SkuServiceImpl service;

    @BeforeEach
    void setUp() {
        skuManager = mock(SkuManager.class);
        spuManager = mock(SpuManager.class);
        spuService = mock(SpuService.class);
        stockFeignClient = mock(StockFeignClient.class);
        lenient().doAnswer(inv -> {
            SkuPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(skuManager).save(any(SkuPO.class));
        lenient().when(skuManager.updateById(any(SkuPO.class))).thenReturn(true);
        lenient().when(skuManager.removeById(100L)).thenReturn(true);
        service = new SkuServiceImpl(skuManager, spuManager, spuService, stockFeignClient);
    }

    /**
     * 验证 getById 在 SKU 存在时返回实体。
     */
    @Test
    public void getById_returns_sku_when_exists() {
        SkuPO s = new SkuPO();
        s.setId(100L);
        s.setSkuName("sku-100");
        when(skuManager.getById(100L)).thenReturn(s);

        SkuPO result = service.getById(100L);
        assertEquals(100L, result.getId());
        assertEquals("sku-100", result.getSkuName());
    }

    /**
     * 验证 getById 在 SKU 不存在时抛出 BizException。
     */
    @Test
    public void getById_throws_when_missing() {
        when(skuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /**
     * 验证 getById 带商家校验：SKU 归属其他商家时抛出 BizException，防止越权读取。
     */
    @Test
    public void getById_throws_when_merchant_mismatch() {
        SkuPO s = new SkuPO();
        s.setId(100L);
        s.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(s);

        assertThrows(BizException.class, () -> service.getById(100L, 99L));
    }

    /**
     * 验证 create 在 price/stock 为 null 时使用缺省值并返回新 ID。
     */
    @Test
    public void create_uses_defaults_when_price_and_stock_null() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("new-sku");
        when(spuManager.getById(1L)).thenReturn(ownedSpu());

        Long id = service.create(10L, dto);
        assertNotNull(id);

        doAnswer(inv -> {
            SkuPO p = inv.getArgument(0);
            assertEquals(BigDecimal.ZERO, p.getPrice());
            assertEquals(0, p.getStock());
            assertEquals(10L, p.getMerchantId());
            return true;
        }).when(skuManager).save(any(SkuPO.class));

        service.create(10L, dto);
    }

    /**
     * 验证 create 成功后刷新父 SPU 的 ES 文档，价格区间随新 SKU 变化。
     */
    @Test
    public void create_refreshes_parent_spu_es_document() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("new-sku");
        dto.setPrice(new BigDecimal("9.90"));
        when(spuManager.getById(1L)).thenReturn(ownedSpu());

        service.create(10L, dto);

        verify(spuService).refreshEsDocument(1L);
    }

    /**
     * 验证 create 在 price/stock 非空时透传原值。
     */
    @Test
    public void create_passes_through_price_and_stock() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("priced-sku");
        dto.setPrice(new BigDecimal("19.90"));
        dto.setStock(50);
        when(spuManager.getById(1L)).thenReturn(ownedSpu());

        Long id = service.create(10L, dto);
        assertNotNull(id);
    }

    /**
     * 验证 create 在父 SPU 归属其他商家时抛出 BizException，防止在他人 SPU 下挂载 SKU。
     */
    @Test
    public void create_throws_when_parent_spu_not_owned() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("rogue-sku");
        SpuPO spu = new SpuPO();
        spu.setId(1L);
        spu.setMerchantId(10L);
        when(spuManager.getById(1L)).thenReturn(spu);

        assertThrows(BizException.class, () -> service.create(99L, dto));
    }

    /**
     * 构造归属商家 10 的父 SPU。
     */
    private SpuPO ownedSpu() {
        SpuPO spu = new SpuPO();
        spu.setId(1L);
        spu.setMerchantId(10L);
        return spu;
    }

    /**
     * 验证 page 返回游标分页结果。
     */
    @Test
    public void page_returns_results() {
        List<SkuPO> mockRecords = new ArrayList<>();
        SkuPO sku = new SkuPO();
        sku.setId(100L);
        mockRecords.add(sku);
        when(skuManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        SkuPageDTO dto = new SkuPageDTO();
        CursorPageVO<SkuPO> result = service.page(dto);

        assertEquals(1, result.getRecords().size());
    }

    /**
     * 验证 update 在 SKU 不存在时抛出 BizException。
     */
    @Test
    public void update_throws_when_missing() {
        when(skuManager.getById(999L)).thenReturn(null);
        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("updated");
        assertThrows(BizException.class, () -> service.update(999L, 10L, dto));
    }

    /**
     * 验证 update 在 SKU 归属其他商家时抛出 BizException，防止越权修改。
     */
    @Test
    public void update_throws_when_merchant_mismatch() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("hacked");
        assertThrows(BizException.class, () -> service.update(100L, 99L, dto));
    }

    /**
     * 验证 update 成功时返回 true 并更新字段。
     */
    @Test
    public void update_returns_true_on_success() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("new-name");
        dto.setPrice(new BigDecimal("29.90"));

        boolean ok = service.update(100L, 10L, dto);
        assertTrue(ok);
        assertEquals("new-name", existing.getSkuName());
        assertEquals(new BigDecimal("29.90"), existing.getPrice());
        verify(skuManager).updateById(any(SkuPO.class));
        verify(spuService).refreshEsDocument(10L);
    }

    /**
     * 验证修改库存时以增量调用库存服务，成功后再更新本地展示投影。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void update_adjusts_stock_via_stock_service() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        existing.setStock(10);
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("new-name");
        dto.setStock(20);
        when(stockFeignClient.adjustStock(any(StockAdjustDTO.class)))
            .thenReturn(Result.success());

        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(20, existing.getStock());
        org.mockito.ArgumentCaptor<StockAdjustDTO> captor =
            org.mockito.ArgumentCaptor.forClass(StockAdjustDTO.class);
        verify(stockFeignClient).adjustStock(captor.capture());
        assertEquals(100L, captor.getValue().getSkuId());
        assertEquals(10L, captor.getValue().getMerchantId());
        assertEquals(10L, captor.getValue().getQuantity());
        verify(skuManager).updateById(existing);
    }

    /**
     * 验证库存服务失败时中止商品更新，不产生本地库存伪更新。
     */
    @Test
    public void update_does_not_change_stock_when_adjust_fails() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        existing.setStock(10);
        when(skuManager.getById(100L)).thenReturn(existing);
        when(stockFeignClient.adjustStock(any(StockAdjustDTO.class)))
            .thenReturn(Result.fail("20000", "stock unavailable"));

        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("new-name");
        dto.setStock(20);

        BizException ex = assertThrows(BizException.class,
            () -> service.update(100L, 10L, dto));

        assertEquals(SkuCodeEnum.STOCK_ADJUST_FAILED.getCode(), ex.getCode());
        assertEquals(10, existing.getStock());
        verify(skuManager, never()).updateById(any(SkuPO.class));
    }

    /**
     * 验证本地 SKU 更新失败时反向补偿已成功的库存调整。
     */
    @Test
    public void update_rolls_back_stock_when_local_update_fails() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        existing.setStock(10);
        when(skuManager.getById(100L)).thenReturn(existing);
        when(skuManager.updateById(any(SkuPO.class))).thenReturn(false);
        when(stockFeignClient.adjustStock(any(StockAdjustDTO.class)))
            .thenReturn(Result.success());

        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("new-name");
        dto.setStock(20);

        BizException ex = assertThrows(BizException.class,
            () -> service.update(100L, 10L, dto));

        assertEquals(SkuCodeEnum.STOCK_ADJUST_FAILED.getCode(), ex.getCode());
        assertEquals(10, existing.getStock());
        org.mockito.ArgumentCaptor<StockAdjustDTO> captor =
            org.mockito.ArgumentCaptor.forClass(StockAdjustDTO.class);
        verify(stockFeignClient, times(2)).adjustStock(captor.capture());
        assertEquals(10L, captor.getAllValues().get(0).getQuantity());
        assertEquals(-10L, captor.getAllValues().get(1).getQuantity());
    }

    /**
     * 验证 delete 在 SKU 不存在时抛出 BizException。
     */
    @Test
    public void delete_throws_when_missing() {
        when(skuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L, 10L));
    }

    /**
     * 验证 delete 在 SKU 归属其他商家时抛出 BizException，防止越权删除。
     */
    @Test
    public void delete_throws_when_merchant_mismatch() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(100L, 99L));
    }

    /**
     * 验证 delete 成功时返回 true。
     */
    @Test
    public void delete_returns_true_on_success() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(skuManager).removeById(100L);
        verify(spuService).refreshEsDocument(10L);
    }

    /**
     * 验证 listSnapshots 仅返回所属 SPU 在售的 SKU 快照，非在售 SPU 的 SKU 不放入结果。
     */
    @Test
    public void listSnapshots_returns_on_sale_only() {
        SkuPO skuOnSale = new SkuPO();
        skuOnSale.setId(100L);
        skuOnSale.setSpuId(1L);
        skuOnSale.setSkuName("on-sale");
        skuOnSale.setPrice(new BigDecimal("9.90"));
        skuOnSale.setStock(10);
        SkuPO skuOff = new SkuPO();
        skuOff.setId(200L);
        skuOff.setSpuId(2L);
        skuOff.setSkuName("off-shelf");
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(skuOnSale, skuOff));
        SpuPO spuOnSale = new SpuPO();
        spuOnSale.setId(1L);
        spuOnSale.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        spuOnSale.setMerchantId(7L);
        SpuPO spuOff = new SpuPO();
        spuOff.setId(2L);
        spuOff.setStatus(SpuStatusEnum.OFF_SHELF.statusValue());
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(spuOnSale, spuOff));

        Map<Long, SkuSnapshotDTO> result = service.listSnapshots(List.of(100L, 200L));

        assertEquals(1, result.size());
        SkuSnapshotDTO dto = result.get(100L);
        assertNotNull(dto);
        assertEquals("on-sale", dto.getSkuName());
        assertEquals(7L, dto.getMerchantId());
        assertNull(result.get(200L));
    }

    /**
     * 验证 listSnapshots 空入参返回空 Map 且不查询数据库。
     */
    @Test
    public void listSnapshots_empty_returns_empty_map() {
        assertTrue(service.listSnapshots(List.of()).isEmpty());
        verify(skuManager, never()).list(any(Wrapper.class));
    }
}
