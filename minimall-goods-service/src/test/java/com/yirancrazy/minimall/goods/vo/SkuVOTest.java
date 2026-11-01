package com.yirancrazy.minimall.goods.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.goods.entity.SkuPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 SkuPo 到 SkuVo 的字段映射及空值保留行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class SkuVoTest {

    /** 验证 SkuPo 全部展示字段被映射到 SkuVo。 */
    public void fromMapsAllFields() {
        SkuPo po = new SkuPo();
        po.setId(1L);
        po.setSpuId(100L);
        po.setSkuName("iPhone 16 Pro 256GB 钛金属原色");
        po.setPrice(new BigDecimal("8999.00"));
        po.setStock(500);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 10, 0, 0);
        po.setCreateTime(now);

        SkuVo vo = SkuVo.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSpuId());
        assertEquals("iPhone 16 Pro 256GB 钛金属原色", vo.getSkuName());
        assertEquals(0, new BigDecimal("8999.00").compareTo(vo.getPrice()));
        assertEquals(500, vo.getStock());
        assertEquals(now, vo.getCreateTime());
    }

    /** 验证 SkuPo 空字段映射后仍保持 null。 */
    public void fromNullFields() {
        SkuPo po = new SkuPo();
        po.setId(1L);
        po.setSpuId(100L);

        SkuVo vo = SkuVo.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals(100L, vo.getSpuId());
        assertNull(vo.getSkuName());
        assertNull(vo.getPrice());
        assertNull(vo.getStock());
        assertNull(vo.getCreateTime());
    }
}
