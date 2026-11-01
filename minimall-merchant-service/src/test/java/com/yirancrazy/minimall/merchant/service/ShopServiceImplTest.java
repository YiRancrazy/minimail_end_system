package com.yirancrazy.minimall.merchant.service;

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
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.ShopStatusEnum;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDto;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDto;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;
import com.yirancrazy.minimall.merchant.entity.ShopPo;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.impl.ShopServiceImpl;
import com.yirancrazy.minimall.merchant.vo.ShopVo;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopServiceImpl 场景测试定义；使用 ShopManager 和 MerchantManager mock 覆盖归属校验、列表、创建、更新、删除和状态限制。当前仅 setUp 标注 @BeforeEach，场景方法缺少 @Test，暂不参与自动执行。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class ShopServiceImplTest {

    private ShopManager shopManager;
    private MerchantManager merchantManager;
    private ShopServiceImpl service;

    /**
     * 初始化店铺 Manager、商家 Manager 和店铺服务实例。
     *
     * 保存回调会为新增店铺回填 ID，updateById 默认返回 true。
     */
    @BeforeEach
    void setUp() {
        shopManager = mock(ShopManager.class);
        merchantManager = mock(MerchantManager.class);
        lenient()
                .doAnswer(
                        inv -> {
                            ShopPo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(shopManager)
                .save(any(ShopPo.class));
        lenient().when(shopManager.updateById(any(ShopPo.class))).thenReturn(true);
        service = new ShopServiceImpl(shopManager, merchantManager);
    }

    /**
     * 构造审核通过的商家测试记录。
     *
     * @param merchantId 商家主键，同时作为用户 ID
     * @return 审核状态为 APPROVED 的商家对象。
     */
    private MerchantPo approvedMerchant(Long merchantId) {
        MerchantPo m = new MerchantPo();
        m.setId(merchantId);
        m.setUserId(merchantId);
        m.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode()));
        return m;
    }

    /**
     * 构造带指定归属的店铺测试记录。
     *
     * @param id 店铺主键
     * @param merchantId 店铺所属商家 ID
     * @return 带固定名称的店铺对象。
     */
    private ShopPo ownerShop(Long id, Long merchantId) {
        ShopPo s = new ShopPo();
        s.setId(id);
        s.setMerchantId(merchantId);
        s.setShopName("shop-" + id);
        return s;
    }

    /**
     * 验证商家可以读取自己的店铺。
     *
     * 前置条件：店铺 10 属于商家 1。关键步骤：以商家 1 调用 getById。预期：返回店铺 10 且名称正确。
     */
    public void getByIdReturnsShopWhenExists() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));

        ShopPo result = service.getById(1L, 10L);
        assertEquals(10L, result.getId());
        assertEquals("shop-10", result.getShopName());
    }

    /**
     * 验证读取不存在的店铺时失败。
     *
     * 前置条件：Manager 查询店铺 999 返回 null。关键步骤：调用 getById。预期：抛出 BizException。
     */
    public void getByIdThrowsWhenMissing() {
        when(shopManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(1L, 999L));
    }

    /**
     * 验证商家不能读取其他商家的店铺。
     *
     * 前置条件：店铺 10 属于商家 2。关键步骤：以商家 1 查询。预期：抛出 BizException。
     */
    public void getByIdThrowsWhenNotOwner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.getById(1L, 10L));
    }

    /**
     * 验证店铺归属为空时不会被视为任意商家所有。
     *
     * 前置条件：店铺 merchantId=null。关键步骤：以商家 1 查询。预期：归属不一致并抛出 BizException。
     */
    public void getByIdThrowsWhenMerchantIdMissing() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, null));
        assertThrows(BizException.class, () -> service.getById(1L, 10L));
    }

    /**
     * 验证店铺存在且归属正确时可以转换为视图。
     *
     * 前置条件：店铺 10 属于商家 1。关键步骤：调用 getVo。预期：视图 ID、商家 ID 和名称与持久化对象一致。
     */
    public void getVoReturnsShopVoWhenExists() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));

        ShopVo result = service.getVo(1L, 10L);

        assertEquals(10L, result.getId());
        assertEquals(1L, result.getMerchantId());
        assertEquals("shop-10", result.getShopName());
    }

    /**
     * 验证视图查询同样执行商家归属校验。
     *
     * 前置条件：店铺 10 属于商家 2。关键步骤：以商家 1 调用 getVo。预期：抛出 BizException。
     */
    public void getVoThrowsWhenNotOwner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.getVo(1L, 10L));
    }

    /**
     * 验证店铺列表查询包含商家 ID 条件。
     *
     * 前置条件：Manager 返回一条店铺记录。关键步骤：以商家 1、limit 20 调用 list 并捕获 QueryWrapper。预期：返回一条记录，SQL 片段包含 merchant_id。
     */
    public void listFiltersByMerchantId() {
        when(shopManager.list(any(QueryWrapper.class)))
                .thenReturn(java.util.List.of(ownerShop(10L, 1L)));

        java.util.List<ShopPo> result = service.list(1L, null, 20);

        assertEquals(1, result.size());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<ShopPo>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(shopManager).list(captor.capture());
        QueryWrapper<ShopPo> q = captor.getValue();
        assertTrue(
                q.getCustomSqlSegment().contains("merchant_id"),
                () -> "sqlSegment=" + q.getCustomSqlSegment());
    }

    /**
     * 验证店铺列表查询结果逐条映射为视图。
     *
     * 前置条件：Manager 返回店铺 10 和 11。关键步骤：调用 listVo。预期：返回两条视图且 ID 顺序保持为 10、11。
     */
    public void listVoMapsEntitiesToViews() {
        when(shopManager.list(any(QueryWrapper.class)))
                .thenReturn(java.util.List.of(ownerShop(10L, 1L), ownerShop(11L, 1L)));

        java.util.List<ShopVo> result = service.listVo(1L, null, 20);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(11L, result.get(1).getId());
    }

    /**
     * 验证审核通过的商家可以创建营业中店铺。
     *
     * 前置条件：商家 1 已通过审核，DTO 合法。关键步骤：调用 create 并捕获保存对象。预期：返回回填 ID，保存对象归属商家 1 且状态为 ACTIVE。
     */
    public void createPersistsAndReturnsId() {
        when(merchantManager.getOne(any())).thenReturn(approvedMerchant(1L));
        ShopCreateDto dto = new ShopCreateDto();
        dto.setShopName("new-shop");
        dto.setLicenseNo("ABC123456789012");

        Long id = service.create(1L, dto);
        assertNotNull(id);
        ArgumentCaptor<ShopPo> captor = ArgumentCaptor.forClass(ShopPo.class);
        verify(shopManager).save(captor.capture());
        assertEquals(1L, captor.getValue().getMerchantId());
        assertEquals(ShopStatusEnum.ACTIVE.intCode(), captor.getValue().getStatus());
    }

    /**
     * 验证商家不存在或审核未通过时不能创建店铺。
     *
     * 前置条件：先返回 null 商家，再返回 REJECTED 商家。关键步骤：分别调用 create。预期：两种情况均抛出 BizException，且不保存店铺。
     */
    public void createThrowsWhenMerchantNotApproved() {
        when(merchantManager.getOne(any())).thenReturn(null);
        ShopCreateDto dto = new ShopCreateDto();
        dto.setShopName("new-shop");
        dto.setLicenseNo("ABC123456789012");

        assertThrows(BizException.class, () -> service.create(1L, dto));
        verify(shopManager, org.mockito.Mockito.never()).save(any(ShopPo.class));

        MerchantPo rejected = new MerchantPo();
        rejected.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.REJECTED.getCode()));
        when(merchantManager.getOne(any())).thenReturn(rejected);
        assertThrows(BizException.class, () -> service.create(1L, dto));
    }

    /**
     * 验证自有店铺更新成功时返回 true。
     *
     * 前置条件：店铺属于商家 1，更新状态为 ACTIVE。关键步骤：调用 update。预期：返回 true 并调用 updateById。
     */
    public void updateReturnsTrueOnSuccess() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        ShopUpdateDto dto = new ShopUpdateDto();
        dto.setShopName("updated-shop");
        dto.setLicenseNo("XYZ987654321098");
        dto.setStatus("ACTIVE");

        boolean ok = service.update(1L, 10L, dto);
        assertTrue(ok);
        verify(shopManager).updateById(any(ShopPo.class));
    }

    /**
     * 验证持久化更新未影响记录时返回 false。
     *
     * 前置条件：归属校验通过，但 Manager.updateById 返回 false。关键步骤：调用 update。预期：返回 false。
     */
    public void updateReturnsFalseOnFailure() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.updateById(any(ShopPo.class))).thenReturn(false);
        ShopUpdateDto dto = new ShopUpdateDto();
        dto.setShopName("x");
        dto.setStatus("ACTIVE");

        boolean ok = service.update(1L, 10L, dto);
        assertFalse(ok);
    }

    /**
     * 验证商家无权将店铺设置为冻结状态。
     *
     * 前置条件：更新 DTO 的 status=SUSPENDED。关键步骤：调用 update。预期：抛出 BizException，且不调用 updateById。
     */
    public void updateThrowsWhenSettingSuspended() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        ShopUpdateDto dto = new ShopUpdateDto();
        dto.setShopName("x");
        dto.setStatus("SUSPENDED");

        assertThrows(BizException.class, () -> service.update(1L, 10L, dto));
        verify(shopManager, org.mockito.Mockito.never()).updateById(any(ShopPo.class));
    }

    /**
     * 验证商家不能更新其他商家的店铺。
     *
     * 前置条件：店铺属于商家 2。关键步骤：以商家 1 调用 update。预期：抛出 BizException，且不执行更新。
     */
    public void updateThrowsWhenNotOwner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        ShopUpdateDto dto = new ShopUpdateDto();
        dto.setShopName("x");

        assertThrows(BizException.class, () -> service.update(1L, 10L, dto));
        verify(shopManager, org.mockito.Mockito.never()).updateById(any(ShopPo.class));
    }

    /**
     * 验证自有店铺删除成功时返回 true。
     *
     * 前置条件：店铺属于商家 1，removeById 返回 true。关键步骤：调用 delete。预期：返回 true 且按店铺 10 执行删除。
     */
    public void deleteReturnsTrueOnSuccess() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.removeById(10L)).thenReturn(true);
        boolean ok = service.delete(1L, 10L);
        assertTrue(ok);
        verify(shopManager).removeById(eq(10L));
    }

    /**
     * 验证持久化删除未影响记录时返回 false。
     *
     * 前置条件：归属校验通过，但 removeById 返回 false。关键步骤：调用 delete。预期：返回 false。
     */
    public void deleteReturnsFalseOnFailure() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 1L));
        when(shopManager.removeById(10L)).thenReturn(false);
        boolean ok = service.delete(1L, 10L);
        assertFalse(ok);
    }

    /**
     * 验证商家不能删除其他商家的店铺。
     *
     * 前置条件：店铺属于商家 2。关键步骤：以商家 1 调用 delete。预期：抛出 BizException，且不调用 removeById。
     */
    public void deleteThrowsWhenNotOwner() {
        when(shopManager.getById(10L)).thenReturn(ownerShop(10L, 2L));
        assertThrows(BizException.class, () -> service.delete(1L, 10L));
        verify(shopManager, org.mockito.Mockito.never()).removeById(any());
    }
}
