package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.fallback.OrderFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Order Feign 客户端，调用Order服务接口
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface OrderFeignClient {
    @GetMapping("/internal/order/{id}")
    String status(@PathVariable("id") Long id);
}