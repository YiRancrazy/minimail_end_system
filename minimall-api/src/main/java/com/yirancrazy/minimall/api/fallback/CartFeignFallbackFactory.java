package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.CartFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartFeign Feign 降级工厂，处理CartFeign服务调用失败降级
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Slf4j
@Component
public class CartFeignFallbackFactory implements FallbackFactory<CartFeignClient> {
    @Override
    public CartFeignClient create(Throwable cause) {
        log.warn("cart-service unreachable, returning sentinel count=-1: {}", cause.getMessage());
        return userId -> -1L;
    }
}