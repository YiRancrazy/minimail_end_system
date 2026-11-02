package com.yirancrazy.minimall.order.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.common.handle.IdempotencyContext;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.order.entity.OrderPo;
import com.yirancrazy.minimall.order.manager.OrderManager;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 验证订单结算重复请求按幂等键返回首次订单或进入处理中分支。
 * @Version: 1.0
 * @DateTime: 2026/11/2 9:12
 */
class OrderCheckoutReplayHandlerTest {

    /** * 验证订单不存在时返回 null，由切面统一返回重复请求错误. */
    @Test
    void testMissingOrderReturnsNull() throws Exception {
        OrderManager orderManager = mock(OrderManager.class);
        Method method =
                OrderCheckoutReplayHandlerTest.class.getDeclaredMethod(
                        "testMissingOrderReturnsNull");
        OrderCheckoutReplayHandler handler = new OrderCheckoutReplayHandler(orderManager);
        when(orderManager.getOrderByIdempotencyKey("order:checkout:1:req-1"))
                .thenReturn(null);

        assertNull(
                handler.onDuplicate(
                        new IdempotencyContext(
                                method, new Object[0], "order:checkout:1:req-1")));
    }

    /** * 验证订单已落库时返回首次订单 ID. */
    @Test
    void testExistingOrderReturnsSameId() throws Exception {
        OrderManager orderManager = mock(OrderManager.class);
        Method method =
                OrderCheckoutReplayHandlerTest.class.getDeclaredMethod(
                        "testExistingOrderReturnsSameId");
        OrderCheckoutReplayHandler handler = new OrderCheckoutReplayHandler(orderManager);
        OrderPo order = new OrderPo();
        order.setId(1001L);
        when(orderManager.getOrderByIdempotencyKey("order:checkout:1:req-1"))
                .thenReturn(order);

        Result<Long> result =
                handler.onDuplicate(
                        new IdempotencyContext(
                                method, new Object[0], "order:checkout:1:req-1"));

        assertEquals("00000", result.getCode());
        assertEquals(1001L, result.getData());
    }
}
