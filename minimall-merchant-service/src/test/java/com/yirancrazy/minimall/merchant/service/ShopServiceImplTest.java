package com.yirancrazy.minimall.merchant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.impl.ShopServiceImpl;

/**
 * ShopServiceImpl 单元测试，覆盖查询、创建、更新、删除的正常、失败、边界路径及商家归属校验。
 */
public class ShopServiceImplTest {

    private ShopManager shopManager;
    private ShopServiceImpl service;

    @BeforeEach
    void setUp() {
        shopManager = mock(ShopManager.class);
        lenient().doAnswer(inv -> {
            ShopPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(shopManager).save(any(ShopPO.class));
        lenient().when(shopManager.updateById(any(ShopPO.class))).thenReturn(true);
        service = new ShopServiceImpl(shopManager);
    }

    private ShopPO ownerShop(Long id, Long merchantId) {
        ShopPO s = new ShopPO();
        s.setId(id);
        s.setMerchantId(merchantId);
        s.setShopName("shop-" + id);
        return s;
    }

    /**
     * 验证 getById 在本商家名下店铺存在时返回实体。
     */
    @Test
    public void getById_returns_shop_when_exists() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));

        ShopPO result = service.getById(1L, 10L);
        assertEquals(10L, result.getId());
        assertEquals("shop-10", result.getShopName());
    }

    /**
     * 验证 getById 在店铺不存在时抛出 BizException。
     */
    @Test
    public void getById_throws_when_missing() {
        when(shopManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(1L, 999L));
    }

    /**
     * 验证 getById 越权访问他人店铺时按"店铺不存在"抛出 BizException。
     */
    @Test
    public void getById_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.getById(1L, 10L));
    }

    /**
     * 验证 getById 对未回填归属的历史数据（merchantId 为空）按"店铺不存在"处理。
     */
    @Test
    public void getById_throws_when_merchant_id_missing() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, null));
        assertThrows(BizException.class, () -> service.getById(1L, 10L));
    }

    /**
     * 验证 list 只查询当前商家名下店铺（merchant_id 条件强制生效）。
     */
    @Test
    public void list_filters_by_merchant_id() {
        when(shopManager.list(any(QueryWrapper.class)))
                .thenReturn(java.util.List.of(ownerShop(10L, 1L)));

        java.util.List<ShopPO> result = service.list(1L, null, 20);

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<ShopPO>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(shopManager).list(captor.capture());
        QueryWrapper<ShopPO> q = captor.getValue();
        assertTrue(q.getCustomSqlSegment().contains("merchant_id"),
                () -> "sqlSegment=" + q.getCustomSqlSegment());
    }

    /**
     * 验证 create 持久化店铺并绑定归属商家、返回新 ID。
     */
    @Test
    public void create_persists_and_returns_id() {
        ShopCreateDTO dto = new ShopCreateDTO();
        dto.setShopName("new-shop");
        dto.setLicenseNo("ABC123456789012");
        dto.setStatus("ACTIVE");

        Long id = service.create(1L, dto);
        assertNotNull(id);
        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopManager).save(captor.capture());
        assertEquals(1L, captor.getValue().getMerchantId());
        assertEquals(com.yirancrazy.minimall.merchant.constant.ShopStatusEnum.ACTIVE.intCode(),
            captor.getValue().getStatus());
    }

    /**
     * 验证 update 在本人名下店铺时调用 updateById 并返回其结果。
     */
    @Test
    public void update_returns_true_on_success() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("updated-shop");
        dto.setLicenseNo("XYZ987654321098");
        dto.setStatus("ACTIVE");

        boolean ok = service.update(1L, 10L, dto);
        assertTrue(ok);
        verify(shopManager).updateById(any(ShopPO.class));
    }

    /**
     * 验证 update 在 updateById 失败时返回 false。
     */
    @Test
    public void update_returns_false_on_failure() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.updateById(any(ShopPO.class))).thenReturn(false);
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("x");
        dto.setStatus("ACTIVE");

        boolean ok = service.update(1L, 10L, dto);
        assertFalse(ok);
    }

    /**
     * 验证 update 越权更新他人店铺时按"店铺不存在"抛出 BizException。
     */
    @Test
    public void update_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("x");

        assertThrows(BizException.class, () -> service.update(1L, 10L, dto));
        verify(shopManager, org.mockito.Mockito.never()).updateById(any(ShopPO.class));
    }

    /**
     * 验证 delete 在本人名下店铺时调用 removeById 并返回其结果。
     */
    @Test
    public void delete_returns_true_on_success() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.removeById(10L)).thenReturn(true);
        boolean ok = service.delete(1L, 10L);
        assertTrue(ok);
        verify(shopManager).removeById(eq(10L));
    }

    /**
     * 验证 delete 在 removeById 失败时返回 false。
     */
    @Test
    public void delete_returns_false_on_failure() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.removeById(10L)).thenReturn(false);
        boolean ok = service.delete(1L, 10L);
        assertFalse(ok);
    }

    /**
     * 验证 delete 越权删除他人店铺时按"店铺不存在"抛出 BizException。
     */
    @Test
    public void delete_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.delete(1L, 10L));
        verify(shopManager, org.mockito.Mockito.never()).removeById(any());
    }
}
