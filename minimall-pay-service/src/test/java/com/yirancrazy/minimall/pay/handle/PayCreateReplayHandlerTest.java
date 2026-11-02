package com.yirancrazy.minimall.pay.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import com.yirancrazy.minimall.pay.manager.PayManager;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证支付创建重放处理器按订单号返回首次支付单。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:14
 */
class PayCreateReplayHandlerTest {

    /** * 验证已有支付流水返回同一主键. */
    @Test
    void testExistingPaymentReturnsSameId() throws Exception {
        PayManager payManager = mock(PayManager.class);
        Method method =
                PayCreateReplayHandlerTest.class.getDeclaredMethod(
                        "testExistingPaymentReturnsSameId");
        PayCreateReplayHandler handler = new PayCreateReplayHandler(payManager);
        PayTransactionPo payment = new PayTransactionPo();
        payment.setId(3001L);
        when(payManager.findLatestByOrderNo("ORDER-1")).thenReturn(payment);

        Result<Long> result =
                handler.onDuplicate(
                        new IdempotencyContext(method, new Object[0], "ORDER-1"));

        assertEquals(3001L, result.getData());
    }

    /** * 验证支付流水尚未可见时返回 null. */
    @Test
    void testMissingPaymentReturnsNull() throws Exception {
        PayManager payManager = mock(PayManager.class);
        Method method =
                PayCreateReplayHandlerTest.class.getDeclaredMethod(
                        "testMissingPaymentReturnsNull");
        PayCreateReplayHandler handler = new PayCreateReplayHandler(payManager);
        when(payManager.findLatestByOrderNo("ORDER-1")).thenReturn(null);

        assertNull(
                handler.onDuplicate(
                        new IdempotencyContext(method, new Object[0], "ORDER-1")));
    }
}
