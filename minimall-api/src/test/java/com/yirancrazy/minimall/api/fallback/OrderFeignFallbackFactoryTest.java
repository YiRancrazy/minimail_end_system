package com.yirancrazy.minimall.api.fallback;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderFeignFallbackFactory 的单元测试类，验证服务不可用时返回系统错误而非伪造成功。
 * @Version: 1.0
 * @DateTime: 2026/8/16
 **/
class OrderFeignFallbackFactoryTest {

    @Test
    void fallback_returnsSysErrorForAllMethods() {
        OrderFeignFallbackFactory f = new OrderFeignFallbackFactory();
        OrderFeignClient client = f.create(new RuntimeException("down"));
        assertNotNull(client);

        Result<Integer> status = client.status(1L);
        assertEquals(CommonCode.SYS_ERROR, status.getCode());
        assertNull(status.getData());

        Result<Long> merchantId = client.merchantId("ORDER1");
        assertEquals(CommonCode.SYS_ERROR, merchantId.getCode());
        assertNull(merchantId.getData());

        Result<Void> pay = client.pay(1L);
        assertEquals(CommonCode.SYS_ERROR, pay.getCode());
        assertNull(pay.getData());

        Result<Void> payByOrderNo = client.payByOrderNo("ORDER1");
        assertEquals(CommonCode.SYS_ERROR, payByOrderNo.getCode());
        assertNull(payByOrderNo.getData());

        Result<Void> refundCallback = client.refundCallback(1L, true);
        assertEquals(CommonCode.SYS_ERROR, refundCallback.getCode());
        assertNull(refundCallback.getData());
    }
}
