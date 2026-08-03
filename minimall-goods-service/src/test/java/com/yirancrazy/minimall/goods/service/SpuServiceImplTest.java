package com.yirancrazy.minimall.goods.service;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.goods.constant.AuditDecisionEnum;
import com.yirancrazy.minimall.goods.constant.SpuStatusEnum;
import com.yirancrazy.minimall.goods.dto.SpuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SpuPageDTO;
import com.yirancrazy.minimall.goods.dto.SpuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SpuAuditRecordPO;
import com.yirancrazy.minimall.goods.entity.SpuPO;
import com.yirancrazy.minimall.goods.manager.SpuAuditRecordManager;
import com.yirancrazy.minimall.goods.manager.SpuManager;
import com.yirancrazy.minimall.goods.search.SpuSearchService;
import com.yirancrazy.minimall.goods.service.impl.SpuServiceImpl;

/**
 * SpuServiceImpl 单元测试，覆盖查询、创建、分页、更新、删除、上下架与审核闭环的正常、失败、边界路径。
 */
public class SpuServiceImplTest {

    private SpuManager spuManager;
    private SpuAuditRecordManager spuAuditRecordManager;
    private SpuSearchService spuSearchService;
    private SpuServiceImpl service;

    @BeforeEach
    void setUp() {
        spuManager = mock(SpuManager.class);
        spuAuditRecordManager = mock(SpuAuditRecordManager.class);
        spuSearchService = mock(SpuSearchService.class);
        lenient().doAnswer(inv -> {
            SpuPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(spuManager).save(any(SpuPO.class));
        lenient().when(spuManager.updateById(any(SpuPO.class))).thenReturn(true);
        lenient().when(spuManager.removeById(100L)).thenReturn(true);
        lenient().when(spuAuditRecordManager.save(any(SpuAuditRecordPO.class))).thenReturn(true);
        service = new SpuServiceImpl(spuManager, spuAuditRecordManager, spuSearchService);
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
     * 验证 create 生成编号、初始为草稿状态并返回新 ID。
     */
    @Test
    public void create_returns_id_and_sets_draft() {
        SpuCreateDTO dto = new SpuCreateDTO();
        dto.setCategoryId(1L);
        dto.setTitle("new-spu");

        Long id = service.create(10L, dto);
        assertNotNull(id);

        doAnswer(inv -> {
            SpuPO p = inv.getArgument(0);
            assertEquals(SpuStatusEnum.DRAFT.statusValue(), p.getStatus());
            assertEquals(10L, p.getMerchantId());
            assertNotNull(p.getSpuNo());
            return true;
        }).when(spuManager).save(any(SpuPO.class));

        service.create(10L, dto);
    }

    /**
     * 验证 page 返回分页结果。
     */
    @Test
    public void page_returns_results() {
        Page<SpuPO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(java.util.List.of(new SpuPO()));
        when(spuManager.page(any(Page.class), any(Wrapper.class))).thenReturn(mockPage);

        SpuPageDTO dto = new SpuPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(20);
        dto.setMerchantId(10L);
        IPage<SpuPO> result = service.page(dto);

        assertEquals(1, result.getRecords().size());
    }

    /**
     * 验证 update 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void update_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("updated");
        assertThrows(BizException.class, () -> service.update(999L, dto));
    }

    /**
     * 验证 update 成功时返回 true 并更新字段。
     */
    @Test
    public void update_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setTitle("old");
        when(spuManager.getById(100L)).thenReturn(existing);

        SpuUpdateDTO dto = new SpuUpdateDTO();
        dto.setTitle("new");
        dto.setSubtitle("sub");
        boolean ok = service.update(100L, dto);

        assertTrue(ok);
        assertEquals("new", existing.getTitle());
        assertEquals("sub", existing.getSubtitle());
        verify(spuManager).updateById(any(SpuPO.class));
    }

    /**
     * 验证 delete 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void delete_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L));
    }

    /**
     * 验证 delete 成功时返回 true。
     */
    @Test
    public void delete_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L);
        assertTrue(ok);
        verify(spuManager).removeById(100L);
    }

    /**
     * 验证 onShelf 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void onShelf_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.onShelf(999L));
    }

    /**
     * 验证 onShelf 在已在售/待审核状态时抛出状态非法异常。
     */
    @Test
    public void onShelf_throws_when_status_invalid() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.onShelf(100L));
    }

    /**
     * 验证 onShelf 在草稿状态时成功提交审核并置为待审核。
     */
    @Test
    public void onShelf_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.onShelf(100L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.PENDING_AUDIT.statusValue(), existing.getStatus());
    }

    /**
     * 验证 offShelf 在 SPU 不存在时抛出 BizException。
     */
    @Test
    public void offShelf_throws_when_missing() {
        when(spuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.offShelf(999L));
    }

    /**
     * 验证 offShelf 在非在售状态时抛出状态非法异常。
     */
    @Test
    public void offShelf_throws_when_status_invalid() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.DRAFT.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        assertThrows(BizException.class, () -> service.offShelf(100L));
    }

    /**
     * 验证 offShelf 在在售状态时成功下架并置为下架。
     */
    @Test
    public void offShelf_returns_true_on_success() {
        SpuPO existing = new SpuPO();
        existing.setId(100L);
        existing.setStatus(SpuStatusEnum.ON_SALE.statusValue());
        when(spuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.offShelf(100L);
        assertTrue(ok);
        assertEquals(SpuStatusEnum.OFF_SHELF.statusValue(), existing.getStatus());
    }

    /**
     * 验证 pagePending 返回待审核分页结果。
     */
    @Test
    public void pagePending_returns_results() {
        Page<SpuPO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(List.of(new SpuPO()));
        when(spuManager.page(any(Page.class), any(Wrapper.class))).thenReturn(mockPage);

        IPage<SpuPO> result = service.pagePending(1, 20);
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
}
