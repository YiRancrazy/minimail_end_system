package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.fallback.CartFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface CartFeignClient {
    @GetMapping("/internal/cart/count")
    Long countByUser(@RequestParam("userId") Long userId);
}