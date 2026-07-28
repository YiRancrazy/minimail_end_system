package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.CartFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CartFeignFallbackFactory implements FallbackFactory<CartFeignClient> {
    @Override
    public CartFeignClient create(Throwable cause) {
        log.warn("cart-service unreachable, returning sentinel count=-1: {}", cause.getMessage());
        return userId -> -1L;
    }
}