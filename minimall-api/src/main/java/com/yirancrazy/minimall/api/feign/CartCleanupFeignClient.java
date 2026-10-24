package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.config.CartCleanupFeignConfiguration;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.fallback.CartCleanupFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车结算清理 Feign 客户端。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@FeignClient(
    name = "minimall-cart-service",
    contextId = "cartCleanupFeignClient",
    configuration = CartCleanupFeignConfiguration.class,
    fallbackFactory = CartCleanupFeignFallbackFactory.class)
public interface CartCleanupFeignClient {

    /**
     * 清理已结算购物车条目。
     * @param dto 清理参数
     * @return 统一响应结果
     */
    @PostMapping("/internal/cart/checkout-cleanup")
    Result<Void> cleanupCheckout(@RequestBody CartCheckoutCleanupDTO dto);
}
