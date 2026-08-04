package com.yirancrazy.minimall.pay.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayTransactionVO 单元测试，验证从 PayTransactionPO 到 PayTransactionVO 的字段映射正确性及内部字段隐藏。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
class PayTransactionVOTest {

    @Test
    void from_mapsAllFields() {
        PayTransactionPO po = new PayTransactionPO();
        po.setPaymentNo("PAY202608040001");
        po.setOrderNo("ORD202608040001");
        po.setUserId(2001L);
        po.setMerchantId(3001L);
        po.setAmount(new BigDecimal("199.99"));
        po.setStatus(1);
        po.setChannel(0);
        po.setCreateTime(LocalDateTime.of(2026, 8, 4, 14, 30, 0));

        PayTransactionVO vo = PayTransactionVO.from(po);

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

    @Test
    void from_hidesInternalFields() {
        int fieldCount = PayTransactionVO.class.getDeclaredFields().length;
        assertEquals(8, fieldCount, "PayTransactionVO should expose exactly 8 fields to hide internal details");
    }
}
