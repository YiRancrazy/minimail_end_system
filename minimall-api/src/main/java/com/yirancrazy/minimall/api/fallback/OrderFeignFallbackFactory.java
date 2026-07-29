package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务 Feign 降级工厂，在订单服务不可用时返回受控降级结果。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Slf4j
@Component
public class OrderFeignFallbackFactory implements FallbackFactory<OrderFeignClient> {
    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("order-service unreachable: {}", cause.getMessage());
        return id -> "DOWN";
    }
}