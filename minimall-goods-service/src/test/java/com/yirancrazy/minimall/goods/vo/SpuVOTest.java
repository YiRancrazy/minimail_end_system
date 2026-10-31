package com.yirancrazy.minimall.goods.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.goods.entity.SpuPo;
import java.time.LocalDateTime;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证 SpuPo 到 SpuVo 的字段映射及空值保留行为。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class SpuVoTest {

  /** 验证 SpuPo 全部展示字段被映射到 SpuVo。 */
  public void fromMapsAllFields() {
    SpuPo po = new SpuPo();
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

    SpuVo vo = SpuVo.from(po);

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

  /** 验证 SpuPo 空字段映射后仍保持 null。 */
  public void fromNullFields() {
    SpuPo po = new SpuPo();
    po.setId(1L);
    po.setSpuNo("SPU20260804002");

    SpuVo vo = SpuVo.from(po);

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
