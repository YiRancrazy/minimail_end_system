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
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.ShopStatusEnum;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.impl.ShopServiceImpl;
import com.yirancrazy.minimall.merchant.vo.ShopVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class ShopServiceImplTest {

    private ShopManager shopManager;
    private MerchantManager merchantManager;
    private ShopServiceImpl service;

    @BeforeEach
    void setUp() {
        shopManager = mock(ShopManager.class);
        merchantManager = mock(MerchantManager.class);
        lenient().doAnswer(inv -> {
            ShopPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(shopManager).save(any(ShopPO.class));
        lenient().when(shopManager.updateById(any(ShopPO.class))).thenReturn(true);
        service = new ShopServiceImpl(shopManager, merchantManager);
    }

    private MerchantPO approvedMerchant(Long merchantId) {
        MerchantPO m = new MerchantPO();
        m.setId(merchantId);
        m.setUserId(merchantId);
        m.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode()));
        return m;
    }

    private ShopPO ownerShop(Long id, Long merchantId) {
        ShopPO s = new ShopPO();
        s.setId(id);
        s.setMerchantId(merchantId);
        s.setShopName("shop-" + id);
        return s;
    }


    /**
     * 获取相关数据。
     */
    public void getById_returns_shop_when_exists() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));

        ShopPO result = service.getById(1L, 10L);
        assertEquals(10L, result.getId());
        assertEquals("shop-10", result.getShopName());
    }


    /**
     * 获取相关数据。
     */
    public void getById_throws_when_missing() {
        when(shopManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(1L, 999L));
    }


    /**
     * 获取相关数据。
     */
    public void getById_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.getById(1L, 10L));
    }


    /**
     * 获取相关数据。
     */
    public void getById_throws_when_merchant_id_missing() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, null));
        assertThrows(BizException.class, () -> service.getById(1L, 10L));
    }


    /**
     * 获取相关数据。
     */
    public void getVO_returns_shop_vo_when_exists() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));

        ShopVO result = service.getVO(1L, 10L);

        assertEquals(10L, result.getId());
        assertEquals(1L, result.getMerchantId());
        assertEquals("shop-10", result.getShopName());
    }


    /**
     * 获取相关数据。
     */
    public void getVO_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.getVO(1L, 10L));
    }


    /**
     * 查询列表数据。
     */
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
     * 查询列表数据。
     */
    public void listVO_maps_entities_to_views() {
        when(shopManager.list(any(QueryWrapper.class)))
                .thenReturn(java.util.List.of(ownerShop(10L, 1L), ownerShop(11L, 1L)));

        java.util.List<ShopVO> result = service.listVO(1L, null, 20);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(11L, result.get(1).getId());
    }


    /**
     * 创建业务数据。
     */
    public void create_persists_and_returns_id() {
        when(merchantManager.getOne(any())).thenReturn(approvedMerchant(1L));
        ShopCreateDTO dto = new ShopCreateDTO();
        dto.setShopName("new-shop");
        dto.setLicenseNo("ABC123456789012");

        Long id = service.create(1L, dto);
        assertNotNull(id);
        ArgumentCaptor<ShopPO> captor = ArgumentCaptor.forClass(ShopPO.class);
        verify(shopManager).save(captor.capture());
        assertEquals(1L, captor.getValue().getMerchantId());
        assertEquals(ShopStatusEnum.ACTIVE.intCode(), captor.getValue().getStatus());
    }


    /**
     * 创建业务数据。
     */
    public void create_throws_when_merchant_not_approved() {
        when(merchantManager.getOne(any())).thenReturn(null);
        ShopCreateDTO dto = new ShopCreateDTO();
        dto.setShopName("new-shop");
        dto.setLicenseNo("ABC123456789012");

        assertThrows(BizException.class, () -> service.create(1L, dto));
        verify(shopManager, org.mockito.Mockito.never()).save(any(ShopPO.class));

        MerchantPO rejected = new MerchantPO();
        rejected.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.REJECTED.getCode()));
        when(merchantManager.getOne(any())).thenReturn(rejected);
        assertThrows(BizException.class, () -> service.create(1L, dto));
    }


    /**
     * 更新业务数据。
     */
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
     * 更新业务数据。
     */
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
     * 更新业务数据。
     */
    public void update_throws_when_setting_suspended() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("x");
        dto.setStatus("SUSPENDED");

        assertThrows(BizException.class, () -> service.update(1L, 10L, dto));
        verify(shopManager, org.mockito.Mockito.never()).updateById(any(ShopPO.class));
    }


    /**
     * 更新业务数据。
     */
    public void update_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("x");

        assertThrows(BizException.class, () -> service.update(1L, 10L, dto));
        verify(shopManager, org.mockito.Mockito.never()).updateById(any(ShopPO.class));
    }


    /**
     * 删除业务数据。
     */
    public void delete_returns_true_on_success() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.removeById(10L)).thenReturn(true);
        boolean ok = service.delete(1L, 10L);
        assertTrue(ok);
        verify(shopManager).removeById(eq(10L));
    }


    /**
     * 删除业务数据。
     */
    public void delete_returns_false_on_failure() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.removeById(10L)).thenReturn(false);
        boolean ok = service.delete(1L, 10L);
        assertFalse(ok);
    }


    /**
     * 删除业务数据。
     */
    public void delete_throws_when_not_owner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.delete(1L, 10L));
        verify(shopManager, org.mockito.Mockito.never()).removeById(any());
    }
}
