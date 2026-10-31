package com.yirancrazy.minimall.api.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.yirancrazy.minimall.api.dto.order.OrderPaySnapshotDto;
import com.yirancrazy.minimall.api.dto.order.RefundCallbackDto;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验订单状态、快照、支付与退款回调降级后返回系统错误.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
class OrderFeignFallbackFactoryTest {

  /** 当前覆盖的订单远程方法在降级时应返回系统错误且不携带数据. */
  @Test
  void testFallbackReturnsSysErrorForAllMethods() {
    OrderFeignFallbackFactory f = new OrderFeignFallbackFactory();
    OrderFeignClient client = f.create(new RuntimeException("down"));
    assertNotNull(client);

    Result<Integer> status = client.status(1L);
    assertEquals(CommonCode.SYS_ERROR, status.getCode());
    assertNull(status.getData());

    Result<Long> merchantId = client.merchantId("ORDER1");
    assertEquals(CommonCode.SYS_ERROR, merchantId.getCode());
    assertNull(merchantId.getData());

    Result<OrderPaySnapshotDto> paySnapshot = client.paySnapshot("ORDER1");
    assertEquals(CommonCode.SYS_ERROR, paySnapshot.getCode());
    assertNull(paySnapshot.getData());

    Result<Void> pay = client.pay(1L);
    assertEquals(CommonCode.SYS_ERROR, pay.getCode());
    assertNull(pay.getData());

    Result<Void> payByOrderNo = client.payByOrderNo("ORDER1");
    assertEquals(CommonCode.SYS_ERROR, payByOrderNo.getCode());
    assertNull(payByOrderNo.getData());

    Result<Void> refundCallback = client.refundCallback(new RefundCallbackDto());
    assertEquals(CommonCode.SYS_ERROR, refundCallback.getCode());
    assertNull(refundCallback.getData());
  }
}
