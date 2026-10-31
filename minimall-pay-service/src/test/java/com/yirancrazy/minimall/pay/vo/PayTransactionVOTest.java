package com.yirancrazy.minimall.pay.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付流水实体到对外视图的字段映射和边界。
 * 测试检查八个业务字段是否完整复制，并防止内部实体字段意外暴露。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class PayTransactionVoTest {

  /** 验证支付视图映射：构造完整实体，预期八个业务字段全部一致. */
  @Test
  void testFromMapsAllFields() {
    PayTransactionPo po = new PayTransactionPo();
    po.setPaymentNo("PAY202608040001");
    po.setOrderNo("ORD202608040001");
    po.setUserId(2001L);
    po.setMerchantId(3001L);
    po.setAmount(new BigDecimal("199.99"));
    po.setStatus(1);
    po.setChannel(0);
    po.setCreateTime(LocalDateTime.of(2026, 8, 4, 14, 30, 0));

    PayTransactionVo vo = PayTransactionVo.from(po);

    assertNotNull(vo);
    assertEquals("PAY202608040001", vo.getPaymentNo());
    assertEquals("ORD202608040001", vo.getOrderNo());
    assertEquals(2001L, vo.getUserId());
    assertEquals(3001L, vo.getMerchantId());
    assertEquals(0, new BigDecimal("199.99").compareTo(vo.getAmount()));
    assertEquals(1, vo.getStatus());
    assertEquals(0, vo.getChannel());
    assertEquals(LocalDateTime.of(2026, 8, 4, 14, 30, 0), vo.getCreateTime());
  }

  /** 验证支付视图边界：反射统计字段数，预期恰为八且不暴露内部字段. */
  @Test
  void testFromHidesInternalFields() {
    int fieldCount = PayTransactionVo.class.getDeclaredFields().length;
    assertEquals(
        8, fieldCount, "PayTransactionVo should expose exactly 8 fields to hide internal details");
  }
}
