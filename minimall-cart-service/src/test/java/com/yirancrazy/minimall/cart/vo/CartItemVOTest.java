package com.yirancrazy.minimall.cart.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemVO 单元测试，覆盖 from 转换方法的字段映射与空值场景。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public class CartItemVOTest {

    /**
     * 验证 from 方法正确映射 CartItemPO 的所有字段。
     */
    @Test
    public void from_mapsAllFields() {
        CartItemPO po = new CartItemPO();
        po.setId(1L);
        po.setUserId(7L);
        po.setSkuId(100L);
        po.setQuantity(2);
        po.setSelected(1);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 10, 0, 0);
        po.setCreateTime(now);

        CartItemVO vo = CartItemVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(7L, vo.getUserId());
        assertEquals(100L, vo.getSkuId());
        assertEquals(2, vo.getQuantity());
        assertEquals(1, vo.getSelected());
        assertEquals(now, vo.getCreateTime());
    }

    /**
     * 验证 from 方法在 PO 字段为 null 时保留 null 值。
     */
    @Test
    public void from_nullFields() {
        CartItemPO po = new CartItemPO();
        po.setId(1L);

        CartItemVO vo = CartItemVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertNull(vo.getUserId());
        assertNull(vo.getSkuId());
        assertNull(vo.getQuantity());
        assertNull(vo.getSelected());
        assertNull(vo.getCreateTime());
    }
}
