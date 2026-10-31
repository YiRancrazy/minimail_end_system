package com.yirancrazy.minimall.order.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.order.entity.OrderPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证订单持久化对象到 OrderVo 的字段映射和状态别名转换。
 *   业务边界：直接构造 OrderPo，不启动 Spring 容器或数据库。
 *   使用场景：覆盖完整字段和可选字段为空两种转换场景。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class OrderVoTest {

  /** 验证OrderVo 完整字段映射. */
  @Test
  void testFromMapsAllFields() {
    OrderPo po = new OrderPo();
    po.setId(1001L);
    po.setOrderNo("OD202608041030");
    po.setUserId(2001L);
    po.setMerchantId(3001L);
    po.setSkuId(4001L);
    po.setQuantity(2);
    po.setAmount(new BigDecimal("99.50"));
    po.setStatus(1);
    po.setReceiverSnapshotJson("{\"receiver\":\"张三\"}");
    po.setCreateTime(LocalDateTime.of(2026, 8, 4, 10, 30, 0));

    OrderVo vo = OrderVo.from(po);

    assertNotNull(vo);
    assertEquals(1001L, vo.getId());
    assertEquals("OD202608041030", vo.getOrderNo());
    assertEquals(2001L, vo.getUserId());
    assertEquals(3001L, vo.getMerchantId());
    assertEquals(4001L, vo.getSkuId());
    assertEquals(2, vo.getQuantity());
    assertEquals("99.50", vo.getAmount());
    assertEquals("PENDING", vo.getStatus());
    assertEquals("{\"receiver\":\"张三\"}", vo.getAddressSnapshot());
    assertEquals(LocalDateTime.of(2026, 8, 4, 10, 30, 0), vo.getCreateTime());
  }

  /** 验证OrderVo 可选字段为空时的映射. */
  @Test
  void testFromNullOptionalFields() {
    OrderPo po = new OrderPo();
    po.setId(1002L);
    po.setUserId(2002L);
    po.setMerchantId(null);
    po.setSkuId(4002L);
    po.setQuantity(1);
    po.setAmount(new BigDecimal("10.00"));
    po.setStatus(0);
    po.setCreateTime(LocalDateTime.of(2026, 8, 4, 11, 0, 0));

    OrderVo vo = OrderVo.from(po);

    assertNotNull(vo);
    assertNull(vo.getMerchantId());
    assertEquals(1002L, vo.getId());
    assertEquals(2002L, vo.getUserId());
    assertEquals(4002L, vo.getSkuId());
    assertEquals(1, vo.getQuantity());
    assertEquals("10.00", vo.getAmount());
    assertEquals("0", vo.getStatus());
    assertEquals(LocalDateTime.of(2026, 8, 4, 11, 0, 0), vo.getCreateTime());
  }
}
