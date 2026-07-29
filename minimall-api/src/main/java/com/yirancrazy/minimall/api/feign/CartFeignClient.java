package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.fallback.CartFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车服务 Feign 客户端，提供跨服务购物车统计能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@FeignClient(value = "minimall-cart-service", fallbackFactory = CartFeignFallbackFactory.class)
public interface CartFeignClient {
    @GetMapping("/internal/cart/count")
    Long countByUser(@RequestParam("userId") Long userId);
}