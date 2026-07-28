package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.fallback.CartFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "minimall-cart-service", fallbackFactory = CartFeignFallbackFactory.class)
public interface CartFeignClient {
    @GetMapping("/internal/cart/count")
    Long countByUser(@RequestParam("userId") Long userId);
}