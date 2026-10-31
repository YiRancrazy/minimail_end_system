package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.api.fallback.CartFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 定义订单结算所需的购物车条目计数与结算快照远程调用.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@FeignClient(name = "minimall-cart-service", fallbackFactory = CartFeignFallbackFactory.class)
public interface CartFeignClient {

  /**
   * 统计指定用户购物车中的有效商品件数.
   *
   * @param userId 用户 ID
   * @return 包装后的购物车商品件数
   */
  @GetMapping("/internal/cart/count")
  Result<Long> countByUser(@RequestParam("userId") Long userId);

  /**
   * 按用户与购物车条目 ID 获取结算所需快照.
   *
   * @param dto 用户及待结算购物车条目 ID
   * @return 包装后的购物车条目快照列表
   */
  @PostMapping("/internal/cart/checkout-snapshot")
  Result<List<CartCheckoutItemDto>> getCheckoutItems(@RequestBody CartCheckoutQueryDto dto);
}
