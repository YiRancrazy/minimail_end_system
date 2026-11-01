package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.config.CartCleanupFeignConfiguration;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDto;
import com.yirancrazy.minimall.api.fallback.CartCleanupFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在订单结算成功后清理购物车条目，并隔离短超时与降级策略.
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@FeignClient(
        name = "minimall-cart-service",
        contextId = "cartCleanupFeignClient",
        configuration = CartCleanupFeignConfiguration.class,
        fallbackFactory = CartCleanupFeignFallbackFactory.class)
public interface CartCleanupFeignClient {

    /**
     * 删除订单结算成功的指定购物车条目，重复清理应保持幂等.
     *
     * @param dto 用户及待清理购物车条目
     * @return 清理成功或系统错误
     */
    @PostMapping("/internal/cart/checkout-cleanup")
    Result<Void> cleanupCheckout(@RequestBody CartCheckoutCleanupDto dto);
}
