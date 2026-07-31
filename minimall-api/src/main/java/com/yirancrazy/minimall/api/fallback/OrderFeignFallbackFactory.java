package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.OrderFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderFeignFallbackFactory description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("order-service unreachable: {}", cause.getMessage());
        return id -> "DOWN";
    }
}