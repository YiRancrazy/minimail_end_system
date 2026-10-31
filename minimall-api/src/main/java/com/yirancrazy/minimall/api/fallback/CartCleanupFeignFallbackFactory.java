package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在购物车清理接口不可用时返回可供订单侧重试的明确失败.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Slf4j
@Component
public class CartCleanupFeignFallbackFactory implements FallbackFactory<CartCleanupFeignClient> {

  /**
   * 为购物车清理远程调用异常创建降级客户端.
   *
   * @param cause 购物车清理远程调用异常
   * @return 购物车清理降级客户端
   */
  @Override
  public CartCleanupFeignClient create(Throwable cause) {
    log.warn("cart-service 清理接口不可达: {}", cause.getMessage());
    return new CartCleanupFeignClient() {
      /**
       * 返回可重试的系统错误，避免订单误判购物车已清理.
       *
       * @param dto 用户及待清理购物车条目
       * @return 系统错误响应
       */
      @Override
      public Result<Void> cleanupCheckout(CartCheckoutCleanupDto dto) {
        return Result.fail(CommonCode.SYS_ERROR, "购物车服务不可用");
      }
    };
  }
}
