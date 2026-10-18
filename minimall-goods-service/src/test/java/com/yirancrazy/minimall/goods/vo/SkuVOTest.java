package com.yirancrazy.minimall.goods.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.goods.entity.SkuPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuVOTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class SkuVOTest {


    /**
     * 执行 from_mapsAllFields 方法。
     */
    public void from_mapsAllFields() {
        SkuPO po = new SkuPO();
        po.setId(1L);
        po.setSpuId(100L);
        po.setSkuName("iPhone 16 Pro 256GB 钛金属原色");
        po.setPrice(new BigDecimal("8999.00"));
        po.setStock(500);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 10, 0, 0);
        po.setCreateTime(now);

        SkuVO vo = SkuVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSpuId());
        assertEquals("iPhone 16 Pro 256GB 钛金属原色", vo.getSkuName());
        assertEquals(0, new BigDecimal("8999.00").compareTo(vo.getPrice()));
        assertEquals(500, vo.getStock());
        assertEquals(now, vo.getCreateTime());
    }


    /**
     * 执行 from_nullFields 方法。
     */
    public void from_nullFields() {
        SkuPO po = new SkuPO();
        po.setId(1L);
        po.setSpuId(100L);

        SkuVO vo = SkuVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSpuId());
        assertNull(vo.getSkuName());
        assertNull(vo.getPrice());
        assertNull(vo.getStock());
        assertNull(vo.getCreateTime());
    }
}
