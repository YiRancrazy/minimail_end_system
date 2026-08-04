package com.yirancrazy.minimall.order.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.order.entity.OrderPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderVO 单元测试，验证从 OrderPO 到 OrderVO 的字段映射正确性。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
class OrderVOTest {

    @Test
    void from_mapsAllFields() {
        OrderPO po = new OrderPO();
        po.setId(1001L);
        po.setUserId(2001L);
        po.setMerchantId(3001L);
        po.setSkuId(4001L);
        po.setQuantity(2);
        po.setAmount(new BigDecimal("99.50"));
        po.setStatus(1);
        po.setCreateTime(LocalDateTime.of(2026, 8, 4, 10, 30, 0));

        OrderVO vo = OrderVO.from(po);

        assertNotNull(vo);
        assertEquals(1001L, vo.getId());
        assertEquals(2001L, vo.getUserId());
        assertEquals(3001L, vo.getMerchantId());
        assertEquals(4001L, vo.getSkuId());
        assertEquals(2, vo.getQuantity());
        assertEquals(0, new BigDecimal("99.50").compareTo(vo.getAmount()));
        assertEquals(1, vo.getStatus());
        assertEquals(LocalDateTime.of(2026, 8, 4, 10, 30, 0), vo.getCreateTime());
    }

    @Test
    void from_nullOptionalFields() {
        OrderPO po = new OrderPO();
        po.setId(1002L);
        po.setUserId(2002L);
        po.setMerchantId(null);
        po.setSkuId(4002L);
        po.setQuantity(1);
        po.setAmount(new BigDecimal("10.00"));
        po.setStatus(0);
        po.setCreateTime(LocalDateTime.of(2026, 8, 4, 11, 0, 0));

        OrderVO vo = OrderVO.from(po);

        assertNotNull(vo);
        assertNull(vo.getMerchantId());
        assertEquals(1002L, vo.getId());
        assertEquals(2002L, vo.getUserId());
        assertEquals(4002L, vo.getSkuId());
        assertEquals(1, vo.getQuantity());
        assertEquals(0, new BigDecimal("10.00").compareTo(vo.getAmount()));
        assertEquals(0, vo.getStatus());
        assertEquals(LocalDateTime.of(2026, 8, 4, 11, 0, 0), vo.getCreateTime());
    }
}
