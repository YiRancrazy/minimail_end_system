package com.yirancrazy.minimall.goods.vo;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.goods.entity.SpuPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuVO 单元测试，覆盖 from 转换方法的字段映射与空值场景。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
public class SpuVOTest {

    /**
     * 验证 from 方法正确映射 SpuPO 的所有字段。
     */
    @Test
    public void from_mapsAllFields() {
        SpuPO po = new SpuPO();
        po.setId(1L);
        po.setSpuNo("SPU20260804001");
        po.setMerchantId(10L);
        po.setCategoryId(20L);
        po.setTitle("iPhone 16 Pro");
        po.setSubtitle("钛金属设计");
        po.setMainImageUrl("https://cdn.example.com/iphone16pro.jpg");
        po.setStatus(1);
        LocalDateTime publishAt = LocalDateTime.of(2026, 8, 4, 9, 0, 0);
        po.setPublishAt(publishAt);
        LocalDateTime createTime = LocalDateTime.of(2026, 8, 4, 8, 0, 0);
        po.setCreateTime(createTime);

        SpuVO vo = SpuVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("SPU20260804001", vo.getSpuNo());
        assertEquals(10L, vo.getMerchantId());
        assertEquals(20L, vo.getCategoryId());
        assertEquals("iPhone 16 Pro", vo.getTitle());
        assertEquals("钛金属设计", vo.getSubtitle());
        assertEquals("https://cdn.example.com/iphone16pro.jpg", vo.getMainImageUrl());
        assertEquals(1, vo.getStatus());
        assertEquals(publishAt, vo.getPublishAt());
        assertEquals(createTime, vo.getCreateTime());
    }

    /**
     * 验证 from 方法在 PO 字段为 null 时保留 null 值。
     */
    @Test
    public void from_nullFields() {
        SpuPO po = new SpuPO();
        po.setId(1L);
        po.setSpuNo("SPU20260804002");

        SpuVO vo = SpuVO.from(po);

        assertNotNull(vo);
        assertEquals(1L, vo.getId());
        assertEquals("SPU20260804002", vo.getSpuNo());
        assertNull(vo.getMerchantId());
        assertNull(vo.getCategoryId());
        assertNull(vo.getTitle());
        assertNull(vo.getSubtitle());
        assertNull(vo.getMainImageUrl());
        assertNull(vo.getStatus());
        assertNull(vo.getPublishAt());
        assertNull(vo.getCreateTime());
    }
}
