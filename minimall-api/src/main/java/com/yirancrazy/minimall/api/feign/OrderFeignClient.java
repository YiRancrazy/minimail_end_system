package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务 Feign 客户端，提供跨服务订单创建与推进能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@FeignClient(value = "minimall-order-service", fallbackFactory = OrderFeignFallbackFactory.class)
public interface OrderFeignClient {
    @GetMapping("/internal/order/{id}")
    String status(@PathVariable("id") Long id);
}