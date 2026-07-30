package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.CartFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
* 购物车服务 Feign 降级工厂，在购物车服务不可用时返回受控降级结果（按当前实现：购物车项计数返回 -1L 哨兵值）。
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