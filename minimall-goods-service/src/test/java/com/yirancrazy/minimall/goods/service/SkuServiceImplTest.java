package com.yirancrazy.minimall.goods.service;

import java.math.BigDecimal;
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
import com.yirancrazy.minimall.goods.dto.SkuCreateDTO;
import com.yirancrazy.minimall.goods.dto.SkuPageDTO;
import com.yirancrazy.minimall.goods.dto.SkuUpdateDTO;
import com.yirancrazy.minimall.goods.entity.SkuPO;
import com.yirancrazy.minimall.goods.manager.SkuManager;
import com.yirancrazy.minimall.goods.service.impl.SkuServiceImpl;

/**
 * SkuServiceImpl 单元测试，覆盖按主键查询与创建 SKU 的正常、失败、边界路径。
 */
public class SkuServiceImplTest {

    private SkuManager skuManager;
    private SkuServiceImpl service;

    @BeforeEach
    void setUp() {
        skuManager = mock(SkuManager.class);
        lenient().doAnswer(inv -> {
            SkuPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(skuManager).save(any(SkuPO.class));
        lenient().when(skuManager.updateById(any(SkuPO.class))).thenReturn(true);
        lenient().when(skuManager.removeById(100L)).thenReturn(true);
        service = new SkuServiceImpl(skuManager);
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
     * 验证 create 在 price/stock 为 null 时使用缺省值并返回新 ID。
     */
    @Test
    public void create_uses_defaults_when_price_and_stock_null() {
        SkuCreateDTO dto = new SkuCreateDTO();
        dto.setSpuId(1L);
        dto.setSkuName("new-sku");

        Long id = service.create(dto);
        assertNotNull(id);

        doAnswer(inv -> {
            SkuPO p = inv.getArgument(0);
            assertEquals(BigDecimal.ZERO, p.getPrice());
            assertEquals(0, p.getStock());
            return true;
        }).when(skuManager).save(any(SkuPO.class));

        service.create(dto);
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

        Long id = service.create(dto);
        assertNotNull(id);
    }

    /**
     * 验证 page 返回分页结果。
     */
    @Test
    public void page_returns_results() {
        Page<SkuPO> mockPage = new Page<>(1, 20);
        mockPage.setRecords(java.util.List.of(new SkuPO()));
        when(skuManager.page(any(Page.class), any(Wrapper.class))).thenReturn(mockPage);

        SkuPageDTO dto = new SkuPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(20);
        IPage<SkuPO> result = service.page(dto);

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
        assertThrows(BizException.class, () -> service.update(999L, dto));
    }

    /**
     * 验证 update 成功时返回 true 并更新字段。
     */
    @Test
    public void update_returns_true_on_success() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        existing.setSkuName("old-name");
        when(skuManager.getById(100L)).thenReturn(existing);

        SkuUpdateDTO dto = new SkuUpdateDTO();
        dto.setSkuName("new-name");
        dto.setPrice(new BigDecimal("29.90"));
        dto.setStock(100);

        boolean ok = service.update(100L, dto);
        assertTrue(ok);
        assertEquals("new-name", existing.getSkuName());
        assertEquals(new BigDecimal("29.90"), existing.getPrice());
        assertEquals(100, existing.getStock());
        verify(skuManager).updateById(any(SkuPO.class));
    }

    /**
     * 验证 delete 在 SKU 不存在时抛出 BizException。
     */
    @Test
    public void delete_throws_when_missing() {
        when(skuManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.delete(999L));
    }

    /**
     * 验证 delete 成功时返回 true。
     */
    @Test
    public void delete_returns_true_on_success() {
        SkuPO existing = new SkuPO();
        existing.setId(100L);
        when(skuManager.getById(100L)).thenReturn(existing);

        boolean ok = service.delete(100L);
        assertTrue(ok);
        verify(skuManager).removeById(100L);
    }
}
