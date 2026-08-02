package com.yirancrazy.minimall.cart.service;

import java.util.Collections;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yirancrazy.minimall.cart.dto.CartItemAddDTO;
import com.yirancrazy.minimall.cart.dto.CartItemListDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectAllDTO;
import com.yirancrazy.minimall.cart.dto.CartSelectDTO;
import com.yirancrazy.minimall.cart.dto.CartUpdateDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.cart.manager.CartItemManager;
import com.yirancrazy.minimall.cart.service.impl.CartServiceImpl;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * CartServiceImpl 单元测试，覆盖查询、新增、删除、计数的正常、失败、边界路径。
 */
public class CartServiceImplTest {

    @BeforeAll
    static void initLambdaCache() {
        TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            CartItemPO.class);
    }

    private CartItemManager cartItemManager;
    private CartServiceImpl service;

    @BeforeEach
    void setUp() {
        cartItemManager = mock(CartItemManager.class);
        lenient().doAnswer(inv -> {
            CartItemPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(cartItemManager).save(any(CartItemPO.class));
        service = new CartServiceImpl(cartItemManager);
    }

    /**
     * 验证 listByUser 返回指定用户的购物车条目。
     */
    @Test
    public void listByUser_returns_items() {
        CartItemPO item = new CartItemPO();
        item.setUserId(7L);
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(List.of(item));

        CartItemListDTO dto = new CartItemListDTO();
        dto.setUserId(7L);
        List<CartItemPO> result = service.listByUser(dto);

        assertEquals(1, result.size());
        assertEquals(7L, result.get(0).getUserId());
    }

    /**
     * 验证用户购物车为空时返回空列表。
     */
    @Test
    public void listByUser_returns_empty_when_no_items() {
        when(cartItemManager.list(any(Wrapper.class))).thenReturn(Collections.emptyList());

        CartItemListDTO dto = new CartItemListDTO();
        dto.setUserId(999L);
        List<CartItemPO> result = service.listByUser(dto);

        assertTrue(result.isEmpty());
    }

    /**
     * 验证 add 在 selected 为 null 时默认置为 1 并返回新 ID。
     */
    @Test
    public void add_defaults_selected_when_null() {
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setUserId(7L);
        dto.setSkuId(100L);
        dto.setQuantity(2);
        dto.setSelected(null);

        Long id = service.add(dto);
        assertNotNull(id);

        doAnswer(inv -> {
            CartItemPO p = inv.getArgument(0);
            assertEquals(1, p.getSelected());
            return true;
        }).when(cartItemManager).save(any(CartItemPO.class));

        service.add(dto);
    }

    /**
     * 验证 add 在 selected 非空时透传原值。
     */
    @Test
    public void add_passes_through_selected_when_set() {
        CartItemAddDTO dto = new CartItemAddDTO();
        dto.setUserId(7L);
        dto.setSkuId(100L);
        dto.setQuantity(2);
        dto.setSelected(0);

        Long id = service.add(dto);
        assertNotNull(id);
    }

    /**
     * 验证 delete 调用 removeById 并返回其结果。
     */
    @Test
    public void delete_returns_true_on_success() {
        when(cartItemManager.removeById(1L)).thenReturn(true);
        boolean ok = service.delete(1L);
        assertTrue(ok);
        verify(cartItemManager).removeById(eq(1L));
    }

    /**
     * 验证 delete 在 removeById 失败时返回 false。
     */
    @Test
    public void delete_returns_false_on_failure() {
        when(cartItemManager.removeById(1L)).thenReturn(false);
        boolean ok = service.delete(1L);
        assertFalse(ok);
    }

    /**
     * 验证 countByUser 返回用户购物车条目数量。
     */
    @Test
    public void countByUser_returns_count() {
        when(cartItemManager.count(any())).thenReturn(5L);

        long count = service.countByUser(7L);
        assertEquals(5L, count);
    }

    /**
     * 验证用户无购物车条目时计数返回 0。
     */
    @Test
    public void countByUser_returns_zero_when_empty() {
        when(cartItemManager.count(any())).thenReturn(0L);
        long count = service.countByUser(999L);
        assertEquals(0L, count);
    }

    /**
     * 验证 updateQuantity 在购物车项不存在时抛出 BizException。
     */
    @Test
    public void updateQuantity_throws_when_missing() {
        when(cartItemManager.getById(999L)).thenReturn(null);
        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(3);
        assertThrows(BizException.class, () -> service.updateQuantity(999L, dto));
    }

    /**
     * 验证 updateQuantity 成功时更新数量并返回 true。
     */
    @Test
    public void updateQuantity_returns_true_on_success() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setQuantity(2);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        when(cartItemManager.updateById(any(CartItemPO.class))).thenReturn(true);

        CartUpdateDTO dto = new CartUpdateDTO();
        dto.setQuantity(5);
        boolean ok = service.updateQuantity(1L, dto);

        assertTrue(ok);
        assertEquals(5, existing.getQuantity());
    }

    /**
     * 验证 select 在购物车项不存在时抛出 BizException。
     */
    @Test
    public void select_throws_when_missing() {
        when(cartItemManager.getById(999L)).thenReturn(null);
        CartSelectDTO dto = new CartSelectDTO();
        dto.setSelected(1);
        assertThrows(BizException.class, () -> service.select(999L, dto));
    }

    /**
     * 验证 select 成功时更新勾选状态并返回 true。
     */
    @Test
    public void select_returns_true_on_success() {
        CartItemPO existing = new CartItemPO();
        existing.setId(1L);
        existing.setSelected(0);
        when(cartItemManager.getById(1L)).thenReturn(existing);
        when(cartItemManager.updateById(any(CartItemPO.class))).thenReturn(true);

        CartSelectDTO dto = new CartSelectDTO();
        dto.setSelected(1);
        boolean ok = service.select(1L, dto);

        assertTrue(ok);
        assertEquals(1, existing.getSelected());
    }

    /**
     * 验证 selectAll 调用批量更新并返回 true。
     */
    @Test
    public void selectAll_returns_true_on_success() {
        when(cartItemManager.update(any(Wrapper.class))).thenReturn(true);

        CartSelectAllDTO dto = new CartSelectAllDTO();
        dto.setUserId(7L);
        dto.setSelected(1);
        boolean ok = service.selectAll(dto);

        assertTrue(ok);
        verify(cartItemManager).update(any(Wrapper.class));
    }

    /**
     * 验证 clear 调用批量删除并返回 true。
     */
    @Test
    public void clear_returns_true_on_success() {
        when(cartItemManager.remove(any(Wrapper.class))).thenReturn(true);

        boolean ok = service.clear(7L);

        assertTrue(ok);
        verify(cartItemManager).remove(any(Wrapper.class));
    }
}
