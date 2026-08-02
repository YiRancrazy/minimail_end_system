package com.yirancrazy.minimall.merchant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.dto.ShopCreateDTO;
import com.yirancrazy.minimall.merchant.dto.ShopUpdateDTO;
import com.yirancrazy.minimall.merchant.entity.ShopPO;
import com.yirancrazy.minimall.merchant.manager.ShopManager;
import com.yirancrazy.minimall.merchant.service.impl.ShopServiceImpl;

/**
 * ShopServiceImpl 单元测试，覆盖查询、创建、更新、删除的正常、失败、边界路径。
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

    /**
     * 验证 getById 在店铺存在时返回实体。
     */
    @Test
    public void getById_returns_shop_when_exists() {
        ShopPO s = new ShopPO();
        s.setId(10L);
        s.setShopName("shop-10");
        when(shopManager.getById(10L)).thenReturn(s);

        ShopPO result = service.getById(10L);
        assertEquals(10L, result.getId());
        assertEquals("shop-10", result.getShopName());
    }

    /**
     * 验证 getById 在店铺不存在时抛出 BizException。
     */
    @Test
    public void getById_throws_when_missing() {
        when(shopManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /**
     * 验证 create 持久化店铺并返回新 ID。
     */
    @Test
    public void create_persists_and_returns_id() {
        ShopCreateDTO dto = new ShopCreateDTO();
        dto.setShopName("new-shop");
        dto.setLicenseNo("ABC123456789012");
        dto.setStatus("ACTIVE");

        Long id = service.create(dto);
        assertNotNull(id);
        verify(shopManager).save(any(ShopPO.class));
    }

    /**
     * 验证 update 调用 updateById 并返回其结果。
     */
    @Test
    public void update_returns_true_on_success() {
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("updated-shop");
        dto.setLicenseNo("XYZ987654321098");
        dto.setStatus("ACTIVE");

        boolean ok = service.update(10L, dto);
        assertTrue(ok);
        verify(shopManager).updateById(any(ShopPO.class));
    }

    /**
     * 验证 update 在 updateById 失败时返回 false。
     */
    @Test
    public void update_returns_false_on_failure() {
        when(shopManager.updateById(any(ShopPO.class))).thenReturn(false);
        ShopUpdateDTO dto = new ShopUpdateDTO();
        dto.setShopName("x");

        boolean ok = service.update(10L, dto);
        assertFalse(ok);
    }

    /**
     * 验证 delete 调用 removeById 并返回其结果。
     */
    @Test
    public void delete_returns_true_on_success() {
        when(shopManager.removeById(10L)).thenReturn(true);
        boolean ok = service.delete(10L);
        assertTrue(ok);
        verify(shopManager).removeById(eq(10L));
    }

    /**
     * 验证 delete 在 removeById 失败时返回 false。
     */
    @Test
    public void delete_returns_false_on_failure() {
        when(shopManager.removeById(10L)).thenReturn(false);
        boolean ok = service.delete(10L);
        assertFalse(ok);
    }
}
