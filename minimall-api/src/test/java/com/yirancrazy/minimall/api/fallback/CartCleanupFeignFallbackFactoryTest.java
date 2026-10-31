package com.yirancrazy.minimall.api.fallback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import org.junit.jupiter.api.Test;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 校验购物车清理接口降级时返回可供订单侧重试的系统错误.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
class CartCleanupFeignFallbackFactoryTest {

  /** 购物车清理服务不可达时应返回系统错误而非成功. */
  @Test
  void testFallbackReturnsRetryableSystemError() {
    CartCleanupFeignFallbackFactory factory = new CartCleanupFeignFallbackFactory();
    CartCleanupFeignClient client = factory.create(new RuntimeException("down"));
    CartCheckoutCleanupDto dto = new CartCheckoutCleanupDto(1L, java.util.List.of());

    assertEquals(CommonCode.SYS_ERROR, client.cleanupCheckout(dto).getCode());
  }
}
