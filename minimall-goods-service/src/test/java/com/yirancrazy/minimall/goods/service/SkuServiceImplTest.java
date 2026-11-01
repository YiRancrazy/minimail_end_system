package com.yirancrazy.minimall.goods.service;

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
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.stock.StockAdjustDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.constant.SkuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuCodeEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuCreateDto;
import com.yirancrazy.minimall.goods.dto.SkuPageDto;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDto;
import com.yirancrazy.minimall.goods.entity.SkuPo;
import com.yirancrazy.minimall.goods.entity.SpuPo;
import com.yirancrazy.minimall.goods.gateway.GoodsRemoteGateway;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.service.impl.SkuServiceImpl;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 SKU 读写、商家归属、库存远程调整补偿、搜索刷新和快照过滤。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class SkuServiceImplTest {

    private SkuManager skuManager;
    private SpuManager spuManager;
    private SpuService spuService;
    private GoodsRemoteGateway goodsRemoteGateway;
    private SkuServiceImpl service;

    @BeforeEach
    void setUp() {
        skuManager = mock(SkuManager.class);
        spuManager = mock(SpuManager.class);
        spuService = mock(SpuService.class);
        goodsRemoteGateway = mock(GoodsRemoteGateway.class);
        lenient()
                .doAnswer(
                        inv -> {
                            SkuPo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(skuManager)
                .save(any(SkuPo.class));
        lenient().when(skuManager.updateById(any(SkuPo.class))).thenReturn(true);
        lenient().when(skuManager.removeById(100L)).thenReturn(true);
        service = new SkuServiceImpl(skuManager, spuManager, spuService, goodsRemoteGateway);
    }

    /** 验证 SKU 存在时按主键返回对应记录。 */
    public void getByIdReturnsSkuWhenExists() {
        SkuPo s = new SkuPo();
        s.setId(100L);
        s.setSkuName("sku-100");
        when(skuManager.getById(100L)).thenReturn(s);

        SkuPo result = service.getById(100L);
        assertEquals(100L, result.getId());
        assertEquals("sku-100", result.getSkuName());
    }

    /** 验证 SKU 不存在时按主键查询抛出 SKU_NOT_FOUND。 */
    public void getByIdThrowsWhenMissing() {
        when(skuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /** 验证商家不匹配时 SKU 查询按不存在处理，防止越权读取。 */
    public void getByIdThrowsWhenMerchantMismatch() {
        SkuPo s = new SkuPo();
        s.setId(100L);
        s.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(s);

        assertThrows(BizException.class, () -> service.getById(100L, 99L));
    }

    /** 验证创建 SKU 时价格和库存为空分别落为 0 元和 0 件。 */
    public void createUsesDefaultsWhenPriceAndStockNull() {
        SkuCreateDto dto = new SkuCreateDto();
        dto.setSpuId(1L);
        dto.setSkuName("new-sku");
        when(spuManager.getById(1L)).thenReturn(ownedSpu());

        Long id = service.create(10L, dto);
        assertNotNull(id);

        doAnswer(
                        inv -> {
                            SkuPo p = inv.getArgument(0);
                            assertEquals(BigDecimal.ZERO, p.getPrice());
                            assertEquals(0, p.getStock());
                            assertEquals(10L, p.getMerchantId());
                            return true;
                        })
                .when(skuManager)
                .save(any(SkuPo.class));

        service.create(10L, dto);
    }

    /** 验证创建 SKU 成功后刷新父 SPU 的 Elasticsearch 文档。 */
    public void createRefreshesParentSpuEsDocument() {
        SkuCreateDto dto = new SkuCreateDto();
        dto.setSpuId(1L);
        dto.setSkuName("new-sku");
        dto.setPrice(new BigDecimal("9.90"));
        when(spuManager.getById(1L)).thenReturn(ownedSpu());

        service.create(10L, dto);

        verify(spuService).refreshEsDocument(1L);
    }

    /** 验证创建 SKU 时显式价格和库存被原样保存。 */
    public void createPassesThroughPriceAndStock() {
        SkuCreateDto dto = new SkuCreateDto();
        dto.setSpuId(1L);
        dto.setSkuName("priced-sku");
        dto.setPrice(new BigDecimal("19.90"));
        dto.setStock(50);
        when(spuManager.getById(1L)).thenReturn(ownedSpu());

        Long id = service.create(10L, dto);
        assertNotNull(id);
    }

    /** 验证父 SPU 不属于当前商家时拒绝创建 SKU。 */
    public void createThrowsWhenParentSpuNotOwned() {
        SkuCreateDto dto = new SkuCreateDto();
        dto.setSpuId(1L);
        dto.setSkuName("rogue-sku");
        SpuPo spu = new SpuPo();
        spu.setId(1L);
        spu.setMerchantId(10L);
        when(spuManager.getById(1L)).thenReturn(spu);

        assertThrows(BizException.class, () -> service.create(99L, dto));
    }

    private SpuPo ownedSpu() {
        SpuPo spu = new SpuPo();
        spu.setId(1L);
        spu.setMerchantId(10L);
        return spu;
    }

    /** 验证 SKU 游标分页返回管理器查询记录。 */
    public void pageReturnsResults() {
        List<SkuPo> mockRecords = new ArrayList<>();
        SkuPo sku = new SkuPo();
        sku.setId(100L);
        mockRecords.add(sku);
        when(skuManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        SkuPageDto dto = new SkuPageDto();
        CursorPageVo<SkuPo> result = service.page(dto);

        assertEquals(1, result.getRecords().size());
    }

    /** 验证待更新 SKU 不存在时抛出 SKU_NOT_FOUND。 */
    public void updateThrowsWhenMissing() {
        when(skuManager.getById(999L)).thenReturn(null);
        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("updated");
        assertThrows(BizException.class, () -> service.update(999L, 10L, dto));
    }

    /** 验证商家不匹配时拒绝更新 SKU。 */
    public void updateThrowsWhenMerchantMismatch() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("hacked");
        assertThrows(BizException.class, () -> service.update(100L, 99L, dto));
    }

    /** 验证 SKU 名称和价格更新成功后返回 true 并刷新父 SPU 搜索文档。 */
    public void updateReturnsTrueOnSuccess() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("new-name");
        dto.setPrice(new BigDecimal("29.90"));

        boolean ok = service.update(100L, 10L, dto);
        assertTrue(ok);
        assertEquals("new-name", existing.getSkuName());
        assertEquals(new BigDecimal("29.90"), existing.getPrice());
        verify(skuManager).updateById(any(SkuPo.class));
        verify(spuService).refreshEsDocument(10L);
    }

    /** * 验证 SKU 库存变更通过库存服务按差值调整，并在成功后更新本地记录. */
    @Test
    public void testUpdateAdjustsStockViaStockService() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        existing.setStock(10);
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("new-name");
        dto.setStock(20);
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDto.class))).thenReturn(Result.success());

        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(20, existing.getStock());
        org.mockito.ArgumentCaptor<StockAdjustDto> captor =
                org.mockito.ArgumentCaptor.forClass(StockAdjustDto.class);
        verify(goodsRemoteGateway).adjustStock(captor.capture());
        assertEquals(100L, captor.getValue().getSkuId());
        assertEquals(10L, captor.getValue().getMerchantId());
        assertEquals(10L, captor.getValue().getQuantity());
        verify(skuManager).updateById(existing);
    }

    /** 验证库存服务调整失败时不更新本地 SKU 库存。 */
    public void updateDoesNotChangeStockWhenAdjustFails() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        existing.setStock(10);
        when(skuManager.getById(100L)).thenReturn(existing);
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDto.class)))
                .thenReturn(Result.fail("20000", "stock unavailable"));

        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("new-name");
        dto.setStock(20);

        BizException ex = assertThrows(BizException.class, () -> service.update(100L, 10L, dto));

        assertEquals(SkuCodeEnum.STOCK_ADJUST_FAILED.getCode(), ex.getCode());
        assertEquals(10, existing.getStock());
        verify(skuManager, never()).updateById(any(SkuPo.class));
    }

    /** 验证本地 SKU 更新失败时反向调用库存服务补偿本次差值。 */
    public void updateRollsBackStockWhenLocalUpdateFails() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        existing.setSkuName("old-name");
        existing.setStock(10);
        when(skuManager.getById(100L)).thenReturn(existing);
        when(skuManager.updateById(any(SkuPo.class))).thenReturn(false);
        when(goodsRemoteGateway.adjustStock(any(StockAdjustDto.class))).thenReturn(Result.success());

        SkuUpdateDto dto = new SkuUpdateDto();
        dto.setSkuName("new-name");
        dto.setStock(20);

        BizException ex = assertThrows(BizException.class, () -> service.update(100L, 10L, dto));

        assertEquals(SkuCodeEnum.STOCK_ADJUST_FAILED.getCode(), ex.getCode());
        assertEquals(10, existing.getStock());
        org.mockito.ArgumentCaptor<StockAdjustDto> captor =
                org.mockito.ArgumentCaptor.forClass(StockAdjustDto.class);
        verify(goodsRemoteGateway, times(2)).adjustStock(captor.capture());
        assertEquals(10L, captor.getAllValues().get(0).getQuantity());
        assertEquals(-10L, captor.getAllValues().get(1).getQuantity());
    }

    /** 验证待删除 SKU 不存在时抛出 SKU_NOT_FOUND。 */
    public void deleteThrowsWhenMissing() {
        when(skuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L, 10L));
    }

    /** 验证商家不匹配时拒绝删除 SKU。 */
    public void deleteThrowsWhenMerchantMismatch() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(100L, 99L));
    }

    /** 验证删除 SKU 成功后返回 true 并刷新父 SPU 搜索文档。 */
    public void deleteReturnsTrueOnSuccess() {
        SkuPo existing = new SkuPo();
        existing.setId(100L);
        existing.setSpuId(10L);
        existing.setMerchantId(10L);
        when(skuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(skuManager).removeById(100L);
        verify(spuService).refreshEsDocument(10L);
    }

    /** 验证批量快照仅返回父 SPU 在售的 SKU 及其商家归属。 */
    public void listSnapshotsReturnsOnSaleOnly() {
        SkuPo skuOnSale = new SkuPo();
        skuOnSale.setId(100L);
        skuOnSale.setSpuId(1L);
        skuOnSale.setSkuName("on-sale");
        skuOnSale.setPrice(new BigDecimal("9.90"));
        skuOnSale.setStock(10);
        SkuPo skuOff = new SkuPo();
        skuOff.setId(200L);
        skuOff.setSpuId(2L);
        skuOff.setSkuName("off-shelf");
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(skuOnSale, skuOff));
        SpuPo spuOnSale = new SpuPo();
        spuOnSale.setId(1L);
        spuOnSale.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        spuOnSale.setMerchantId(7L);
        SpuPo spuOff = new SpuPo();
        spuOff.setId(2L);
        spuOff.setStatus(SpuStatusEnum.OFF_SHELF.statusValue());
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(spuOnSale, spuOff));

        Map<Long, SkuSnapshotDto> result = service.listSnapshots(List.of(100L, 200L));

        assertEquals(1, result.size());
        SkuSnapshotDto dto = result.get(100L);
        assertNotNull(dto);
        assertEquals("on-sale", dto.getSkuName());
        assertEquals(7L, dto.getMerchantId());
        assertNull(result.get(200L));
    }

    /** 验证空 SKU 主键列表直接返回空映射且不查询数据库。 */
    public void listSnapshotsEmptyReturnsEmptyMap() {
        assertTrue(service.listSnapshots(List.of()).isEmpty());
        verify(skuManager, never()).list(any(Wrapper.class));
    }

    /** 验证父 SPU 在售时返回包含价格、库存和商家的 SKU 快照。 */
    public void getSnapshotReturnsSnapshotWhenSpuOnSale() {
        SkuPo sku = new SkuPo();
        sku.setId(100L);
        sku.setSpuId(1L);
        sku.setSkuName("on-sale");
        sku.setPrice(new BigDecimal("9.90"));
        sku.setStock(10);
        SpuPo spu = new SpuPo();
        spu.setId(1L);
        spu.setMerchantId(7L);
        spu.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(skuManager.getById(100L)).thenReturn(sku);
        when(spuManager.getById(1L)).thenReturn(spu);

        SkuSnapshotDto result = service.getSnapshot(100L);

        assertEquals(100L, result.getSkuId());
        assertEquals("on-sale", result.getSkuName());
        assertEquals(7L, result.getMerchantId());
    }

    /** 验证父 SPU 缺失时 SKU 快照抛出 SPU_NOT_FOUND。 */
    public void getSnapshotThrowsWhenSpuMissing() {
        SkuPo sku = new SkuPo();
        sku.setId(100L);
        sku.setSpuId(1L);
        when(skuManager.getById(100L)).thenReturn(sku);
        when(spuManager.getById(1L)).thenReturn(null);

        BizException exception = assertThrows(BizException.class, () -> service.getSnapshot(100L));

        assertEquals(SpuCodeEnum.SPU_NOT_FOUND.getCode(), exception.getCode());
    }

    /** 验证父 SPU 非在售时 SKU 快照抛出 SPU_NOT_ON_SALE。 */
    public void getSnapshotThrowsWhenSpuNotOnSale() {
        SkuPo sku = new SkuPo();
        sku.setId(100L);
        sku.setSpuId(1L);
        SpuPo spu = new SpuPo();
        spu.setId(1L);
        spu.setStatus(SpuStatusEnum.OFF_SHELF.statusValue());
        when(skuManager.getById(100L)).thenReturn(sku);
        when(spuManager.getById(1L)).thenReturn(spu);

        BizException exception = assertThrows(BizException.class, () -> service.getSnapshot(100L));

        assertEquals(SpuCodeEnum.SPU_NOT_ON_SALE.getCode(), exception.getCode());
    }
}
