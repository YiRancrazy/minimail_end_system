package com.yirancrazy.minimall.pay.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import com.yirancrazy.minimall.pay.manager.PayRefundManager;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证退款重放处理器区分最终状态、处理中和不存在。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:15
 */
class PayRefundReplayHandlerTest {

    /** * 验证已完成退款返回成功结果. */
    @Test
    void testSuccessRefundReturnsTrue() throws Exception {
        PayRefundManager manager = mock(PayRefundManager.class);
        Method method =
                PayRefundReplayHandlerTest.class.getDeclaredMethod(
                        "testSuccessRefundReturnsTrue");
        PayRefundReplayHandler handler = new PayRefundReplayHandler(manager);
        PayRefundPo refund = new PayRefundPo();
        refund.setStatus(1);
        when(manager.findByRefundRequestNo("REF-1")).thenReturn(refund);

        Result<Boolean> result =
                handler.onDuplicate(
                        new IdempotencyContext(method, new Object[0], "REF-1"));

        assertEquals(Boolean.TRUE, result.getData());
    }

    /** * 验证待处理退款不提前回放成功. */
    @Test
    void testPendingRefundReturnsNull() throws Exception {
        PayRefundManager manager = mock(PayRefundManager.class);
        Method method =
                PayRefundReplayHandlerTest.class.getDeclaredMethod(
                        "testPendingRefundReturnsNull");
        PayRefundReplayHandler handler = new PayRefundReplayHandler(manager);
        PayRefundPo refund = new PayRefundPo();
        refund.setStatus(0);
        when(manager.findByRefundRequestNo("REF-1")).thenReturn(refund);

        assertNull(
                handler.onDuplicate(
                        new IdempotencyContext(method, new Object[0], "REF-1")));
    }

    /** * 验证退款记录不存在时返回 null. */
    @Test
    void testMissingRefundReturnsNull() throws Exception {
        PayRefundManager manager = mock(PayRefundManager.class);
        Method method =
                PayRefundReplayHandlerTest.class.getDeclaredMethod(
                        "testMissingRefundReturnsNull");
        PayRefundReplayHandler handler = new PayRefundReplayHandler(manager);
        when(manager.findByRefundRequestNo("REF-1")).thenReturn(null);

        assertNull(
                handler.onDuplicate(
                        new IdempotencyContext(method, new Object[0], "REF-1")));
    }
}
