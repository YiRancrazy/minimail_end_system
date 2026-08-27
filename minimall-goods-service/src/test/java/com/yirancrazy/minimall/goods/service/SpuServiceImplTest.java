package com.yirancrazy.minimall.goods.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.merchant.ShopSnapshotDTO;
import com.yirancrazy.minimall.api.feign.MerchantFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SkuItemDTO;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuDocument;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.impl.SpuServiceImpl;
import com.yirancrazy.minimall.goods.vo.SpuVO;

/**
 * SpuServiceImpl 单元测试，覆盖查询、创建、分页、更新、删除、上下架与审核闭环的正常、失败、边界路径。
 */
public class SpuServiceImplTest {

    private SpuManager spuManager;
    private SpuAuditRecordManager spuAuditRecordManager;
    private SpuSearchService spuSearchService;
    private SkuManager skuManager;
    private MerchantFeignClient merchantFeignClient;
    private MinioUtil minioUtil;
    private SpuServiceImpl service;

    @BeforeEach
    void setUp() {
        spuManager = mock(SpuManager.class);
        spuAuditRecordManager = mock(SpuAuditRecordManager.class);
        spuSearchService = mock(SpuSearchService.class);
        skuManager = mock(SkuManager.class);
        merchantFeignClient = mock(MerchantFeignClient.class);
        minioUtil = mock(MinioUtil.class);
        lenient().doAnswer(inv -> {
            SpuPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(spuManager).save(any(SpuPO.class));
        lenient().when(spuManager.updateById(any(SpuPO.class))).thenReturn(true);
        lenient().when(spuManager.removeById(100L)).thenReturn(true);
        lenient().when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());
        lenient().when(spuAuditRecordManager.save(any(SpuAuditRecordPO.class))).thenReturn(true);
        // 默认店铺快照：店铺1归属商家10且营业中，覆盖正常路径
        lenient().when(merchantFeignClient.shopSnapshot(anyLong()))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 10L, "shop-1", "ACTIVE")));
        service = new SpuServiceImpl(spuManager, spuAuditRecordManager,
            spuSearchService, skuManager, merchantFeignClient, minioUtil);
    }

    /**
     * 验证 getById 在 SPU 存在时返回实体。
     */
    @Test
    public void getById_returns_spu_when_exists() {
        SpuPO s = new SpuPO();
        s.setId(100L);
        s.setTitle("spu-100");
        when(spuManager.getById(100L)).thenReturn(s);

        SpuPO result = service.getById(100L);
        assertEquals(100L, result.getId());
        assertEquals("spu-100", result.getTitle());
    }

    /**
     * 验证 getById 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void getById_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /**
     * 验证 getById 带商家校验：SPU 归属其他商家时抛出 BizException，防止越权读取。
     */
    @Test
    public void getById_throws_when_merchant_mismatch() {
        SpuPO s = new SpuPO();
        s.setId(100L);
        s.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(s);

        assertThrows(BizException.class, () -> service.getById(100L, 99L));
    }

    /**
     * 验证 getById 带商家校验：SPU 归属本人时正常返回。
     */
    @Test
    public void getById_returns_spu_when_owned() {
        SpuPO s = new SpuPO();
        s.setId(100L);
        s.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(s);

        SpuPO result = service.getById(100L, 10L);
        assertEquals(100L, result.getId());
    }

    /**
     * 验证 create 生成编号、初始为草稿状态并返回新 ID。
     */
    @Test
    public void create_returns_id_and_sets_draft() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        Long id = service.create(10L, dto);
        assertNotNull(id);

        doAnswer(inv -> {
            SpuPO p = inv.getArgument(0);
            assertEquals(SpuStatusEnum.DRAFT.statusValue(), p.getStatus());
            assertEquals(10L, p.getMerchantId());
            assertEquals(1L, p.getShopId());
            assertNotNull(p.getSpuNo());
            return true;
        }).when(spuManager).save(any(SpuPO.class));

        service.create(10L, dto);
    }

    /**
     * 验证 create 携带 SKU 清单时批量保存，并回填 spuId 与商家ID，价格/库存原样落库。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void create_saves_skus_with_spu_id() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        SkuItemDTO item = new SkuItemDTO();
        item.setSkuName("颜色/黑");
        item.setPrice(new BigDecimal("19.90"));
        item.setStock(5);
        dto.setSkus(List.of(item));

        service.create(10L, dto);

        ArgumentCaptor<List<SkuPO>> captor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).saveBatch(captor.capture());
        SkuPO saved = captor.getValue().get(0);
        assertEquals(10L, saved.getMerchantId());
        assertEquals("颜色/黑", saved.getSkuName());
        assertEquals(new BigDecimal("19.90"), saved.getPrice());
        assertEquals(5, saved.getStock());
        assertNotNull(saved.getSpuId());
    }

    /**
     * 验证 create 未携带 SKU 清单时不触发批量保存，兼容仅建 SPU 的场景。
     */
    @Test
    public void create_without_skus_skips_batch_save() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        service.create(10L, dto);

        verify(skuManager, org.mockito.Mockito.never()).saveBatch(anyList());
    }

    /**
     * 验证 create 缺店铺（shopId 为空）时拒绝，无店铺商家不能发布商品。
     */
    @Test
    public void create_throws_when_shop_id_missing() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        assertThrows(BizException.class, () -> service.create(10L, dto));
        verify(spuManager, org.mockito.Mockito.never()).save(any(SpuPO.class));
    }

    /**
     * 验证 create 绑定他人店铺时拒绝，防止越权把商品挂到别的商家店铺下。
     */
    @Test
    public void create_throws_when_shop_not_owned() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(merchantFeignClient.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 99L, "shop-1", "ACTIVE")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }

    /**
     * 验证 create 绑定停业/冻结店铺时拒绝。
     */
    @Test
    public void create_throws_when_shop_inactive() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(merchantFeignClient.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 10L, "shop-1", "INACTIVE")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }

    /**
     * 验证 create 在 merchant-service 不可用（Feign 降级哨兵）时拒绝发布，fail-closed 而非放行。
     */
    @Test
    public void create_throws_when_shop_service_down() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setShopId(1L);
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");
        when(merchantFeignClient.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(-1L, -1L, "unknown", "DOWN")));

        assertThrows(BizException.class, () -> service.create(10L, dto));
    }

    /**
     * 验证 getDetail 装配 SKU 列表，供商家端编辑页回显。
     */
    @Test
    public void getDetail_assembles_skus() {
        SpuPO po = new SpuPO();
        po.setId(100L);
        po.setMerchantId(10L);
        po.setTitle("t");
        when(spuManager.getById(100L)).thenReturn(po);
        SkuPO sku = new SkuPO();
        sku.setId(1L);
        sku.setSpuId(100L);
        sku.setSkuName("颜色/黑");
        sku.setPrice(new BigDecimal("19.90"));
        sku.setStock(5);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(sku));

        SpuVO vo = service.getDetail(100L, 10L);

        assertEquals(1, vo.getSkus().size());
        assertEquals("颜色/黑", vo.getSkus().get(0).getSkuName());
        assertEquals(5, vo.getSkus().get(0).getStock());
    }

    /**
     * 验证 update 携带 skus 时按 id 增改、未出现者删除：已有 SKU 更新价格/库存，无 id 新建，被移除 SKU 删除。
     */
    @SuppressWarnings("unchecked")
    @Test
    public void update_syncs_skus() {
        SpuPO po = new SpuPO();
        po.setId(100L);
        po.setMerchantId(10L);
        po.setStatus(SpuStatusEnum.DRAFT.statusValue());
        po.setCreateTime(LocalDateTime.now());
        when(spuManager.getById(100L)).thenReturn(po);

        SkuPO keptSku = new SkuPO();
        keptSku.setId(1L);
        keptSku.setSpuId(100L);
        keptSku.setSkuName("旧规格");
        SkuPO goneSku = new SkuPO();
        goneSku.setId(2L);
        goneSku.setSpuId(100L);
        goneSku.setSkuName("被移除");
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(keptSku, goneSku));

        SpuUpdateDTO dto = new SpuUpdateDTO();
        SkuItemDTO kept = new SkuItemDTO();
        kept.setId(1L);
        kept.setSkuName("新规格");
        kept.setPrice(new BigDecimal("29.90"));
        kept.setStock(9);
        SkuItemDTO added = new SkuItemDTO();
        added.setSkuName("新增规格");
        added.setPrice(new BigDecimal("9.90"));
        added.setStock(2);
        dto.setSkus(List.of(kept, added));

        service.update(100L, 10L, dto);

        ArgumentCaptor<List<SkuPO>> createCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).saveBatch(createCaptor.capture());
        assertEquals(1, createCaptor.getValue().size());
        assertEquals("新增规格", createCaptor.getValue().get(0).getSkuName());

        ArgumentCaptor<List<SkuPO>> updateCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).updateBatchById(updateCaptor.capture());
        SkuPO updated = updateCaptor.getValue().get(0);
        assertEquals("新规格", updated.getSkuName());
        assertEquals(new BigDecimal("29.90"), updated.getPrice());
        assertEquals(9, updated.getStock());

        ArgumentCaptor<List<Long>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(skuManager).removeByIds(deleteCaptor.capture());
        assertEquals(List.of(2L), deleteCaptor.getValue());
    }

    /**
     * 验证 page 返回游标分页结果并装配 SKU。
     */
    @Test
    public void page_returns_results() {
        SpuPO po = new SpuPO();
        po.setId(1L);
        po.setTitle("t");
        List<SpuPO> mockRecords = new ArrayList<>();
        mockRecords.add(po);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of());

        SpuPageDTO dto = new SpuPageDTO();
        dto.setLimit(20);
        dto.setMerchantId(10L);
        CursorPageVO<SpuVO> result = service.page(dto);

        assertEquals(1, result.getRecords().size());
        assertEquals("t", result.getRecords().get(0).getTitle());
        assertTrue(result.getRecords().get(0).getSkus().isEmpty());
    }

    /**
     * 验证 update 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void update_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("updated");
        assertThrows(BizException.class, () -> service.update(999L, 10L, dto));
    }

    /**
     * 验证 update 在 SPU 归属其他商家时抛出 BizException，防止越权修改。
     */
    @Test
    public void update_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        assertThrows(BizException.class, () -> service.update(100L, 99L, dto));
    }

    /**
     * 验证 update 成功时返回 true 并更新字段。
     */
    @Test
    public void update_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("old");
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        dto.setSubtitle("sub");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals("new", existing.getTitle());
        assertEquals("sub", existing.getSubtitle());
        verify(spuManager).updateById(any(SpuPO.class));
    }

    /**
     * 验证在售商品编辑后状态置回待审核，且 ES 镜像 saleStatus 同步为非在售、退出前台搜索结果。
     */
    @Test
    public void update_on_sale_returns_to_pending_audit() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("old");
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
        ArgumentCaptor<SpuDocument> captor = ArgumentCaptor.forClass(SpuDocument.class);
        verify(spuSearchService).sync(captor.capture());
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(),
            captor.getValue().getSaleStatus());
    }

    /**
     * 验证已驳回商品编辑后状态置回待审核，可重新送审。
     */
    @Test
    public void update_rejected_returns_to_pending_audit() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.REJECTED.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("revised");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }

    /**
     * 验证草稿商品编辑后保持草稿状态，不进入审核流。
     */
    @Test
    public void update_draft_keeps_draft() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("still-draft");
        boolean ok = service.update(100L, 10L, dto);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.DRAFT.statusValue(), existing.getStatus());
    }

    /**
     * 验证 delete 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void delete_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L, 10L));
    }

    /**
     * 验证 delete 在 SPU 归属其他商家时抛出 BizException，防止越权删除。
     */
    @Test
    public void delete_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.delete(100L, 99L));
    }

    /**
     * 验证 delete 成功时返回 true。
     */
    @Test
    public void delete_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(spuManager).removeById(100L);
    }

    /**
     * 验证 delete 成功后同步删除 ES 镜像文档，避免搜索结果残留已删除商品。
     */
    @Test
    public void delete_removes_es_document() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L, 10L);
        assertTrue(ok);
        verify(spuSearchService).deleteById(100L);
    }

    /**
     * 验证 syncToEs 聚合 SPU 全部 SKU 的 min/max 价格写入 ES 文档，null 价格被过滤，元转分。
     */
    @Test
    public void sync_to_es_fills_price_range_from_skus() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setTitle("priced-spu");
        when(spuManager.getById(100L)).thenReturn(existing);

        SkuPO low = new SkuPO();
        low.setPrice(new BigDecimal("9.90"));
        SkuPO high = new SkuPO();
        high.setPrice(new BigDecimal("19.90"));
        SkuPO noPrice = new SkuPO();
        noPrice.setPrice(null);
        when(skuManager.list(any(Wrapper.class))).thenReturn(List.of(low, high, noPrice));

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("updated");
        service.update(100L, 10L, dto);

        ArgumentCaptor<SpuDocument> captor = ArgumentCaptor.forClass(SpuDocument.class);
        verify(spuSearchService).sync(captor.capture());
        SpuDocument doc = captor.getValue();
        assertEquals(990L, doc.getMinPrice());
        assertEquals(1990L, doc.getMaxPrice());
    }

    /**
     * 验证 onShelf 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void onShelf_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.onShelf(999L, 10L));
    }

    /**
     * 验证 onShelf 在 SPU 归属其他商家时抛出 BizException，防止越权上架。
     */
    @Test
    public void onShelf_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L, 99L));
    }

    /**
     * 验证 onShelf 在已在售/待审核状态时抛出状态非法异常。
     */
    @Test
    public void onShelf_throws_when_status_invalid() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L, 10L));
    }

    /**
     * 验证 onShelf 在草稿状态时成功提交审核并置为待审核。
     */
    @Test
    public void onShelf_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.onShelf(100L, 10L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }

    /**
     * 验证 onShelf 绑定店铺非营业中（如被冻结）时拒绝送审，防止店铺停业后仍发布商品。
     */
    @Test
    public void onShelf_throws_when_shop_not_active() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setShopId(1L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);
        when(merchantFeignClient.shopSnapshot(1L))
            .thenReturn(Result.success(new ShopSnapshotDTO(1L, 10L, "shop-1", "SUSPENDED")));

        assertThrows(BizException.class, () -> service.onShelf(100L, 10L));
        verify(spuManager, org.mockito.Mockito.never()).updateById(any(SpuPO.class));
    }

    /**
     * 验证 offShelf 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void offShelf_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.offShelf(999L, 10L));
    }

    /**
     * 验证 offShelf 在 SPU 归属其他商家时抛出 BizException，防止越权下架。
     */
    @Test
    public void offShelf_throws_when_merchant_mismatch() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L, 99L));
    }

    /**
     * 验证 offShelf 在非在售状态时抛出状态非法异常。
     */
    @Test
    public void offShelf_throws_when_status_invalid() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L, 10L));
    }

    /**
     * 验证 offShelf 在在售状态时成功下架并置为下架。
     */
    @Test
    public void offShelf_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setMerchantId(10L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.offShelf(100L, 10L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.OFF_SHELF.statusValue(), existing.getStatus());
    }

    /**
     * 验证 pagePending 返回待审核游标分页结果。
     */
    @Test
    public void pagePending_returns_results() {
        List<SpuPO> mockRecords = new ArrayList<>();
        SpuPO spu = new SpuPO();
        spu.setId(100L);
        mockRecords.add(spu);
        when(spuManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        SpuPageDTO dto = new SpuPageDTO();
        CursorPageVO<SpuPO> result = service.pagePending(dto);
        assertEquals(1, result.getRecords().size());
    }

    /**
     * 验证 approve 在待审核状态时通过并置为在售、记录审核日志。
     */
    @Test
    public void approve_success_sets_on_sale_and_records_log() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.approve(100L, 888L);

        assertTrue(ok);
        assertEquals(SpuStatusEnum.ON_SALE.statusValue(), existing.getStatus());
        assertNotNull(existing.getPublishAt());
        verify(spuAuditRecordManager).save(any(SpuAuditRecordPO.class));
    }

    /**
     * 验证 approve 在非待审核状态时抛出 BizException。
     */
    @Test
    public void approve_throws_when_not_pending() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.approve(100L, 888L));
    }

    /**
     * 验证 approve 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void approve_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.approve(999L, 888L));
    }

    /**
     * 验证 reject 在待审核状态时驳回并置为驳回、记录含原因的审核日志。
     */
    @Test
    public void reject_success_sets_rejected_and_records_log() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.PENDING_AUDIT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.reject(100L, 888L, "图片不合规");

        assertTrue(ok);
        assertEquals(SpuStatusEnum.REJECTED.statusValue(), existing.getStatus());
        verify(spuAuditRecordManager).save(any(SpuAuditRecordPO.class));
    }

    /**
     * 验证 reject 在非待审核状态时抛出 BizException。
     */
    @Test
    public void reject_throws_when_not_pending() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.reject(100L, 888L, "reason"));
    }

    /**
     * 验证 listAuditRecords 返回指定 SPU 的审核记录列表。
     */
    @Test
    public void listAuditRecords_returns_records() {
        SpuAuditRecordPO rec = new SpuAuditRecordPO();
        rec.setSpuId(100L);
        rec.setDecision(AuditDecisionEnum.APPROVE.getCode());
        when(spuAuditRecordManager.list(any(Wrapper.class))).thenReturn(List.of(rec));

        List<SpuAuditRecordPO> records = service.listAuditRecords(100L);

        assertEquals(1, records.size());
        assertEquals(AuditDecisionEnum.APPROVE.getCode(), records.get(0).getDecision());
    }

    /**
     * 验证 listSnapshots 返回 spuId -> 快照 Map，不存在的 SPU 不放入结果。
     */
    @Test
    public void listSnapshots_returns_map() {
        SpuPO spu1 = new SpuPO();
        spu1.setId(1L);
        spu1.setTitle("标题一");
        spu1.setMainImageUrl("http://img/1.jpg");
        SpuPO spu2 = new SpuPO();
        spu2.setId(2L);
        spu2.setTitle("标题二");
        when(spuManager.list(any(Wrapper.class))).thenReturn(List.of(spu1, spu2));

        Map<Long, SpuSnapshotDTO> result = service.listSnapshots(List.of(1L, 2L, 3L));

        assertEquals(2, result.size());
        assertEquals("标题一", result.get(1L).getTitle());
        assertEquals("http://img/1.jpg", result.get(1L).getMainImageUrl());
        assertEquals("标题二", result.get(2L).getTitle());
        assertNull(result.get(3L));
    }

    /**
     * 验证 listSnapshots 空入参返回空 Map。
     */
    @Test
    public void listSnapshots_empty_returns_empty_map() {
        assertTrue(service.listSnapshots(List.of()).isEmpty());
    }
}
