package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutCleanupDTO;
import com.yirancrazy.minimall.api.feign.CartCleanupFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车清理 Feign 降级工厂。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 **/
@Slf4j
@Component
public class CartCleanupFeignFallbackFactory
        implements FallbackFactory<CartCleanupFeignClient> {

    /**
     * 创建购物车清理降级客户端。
     * @param cause 远程调用失败原因
     * @return 降级客户端
     */
    @Override
    public CartCleanupFeignClient create(Throwable cause) {
        log.warn("cart-service 清理接口不可达: {}", cause.getMessage());
        return new CartCleanupFeignClient() {
            /**
             * 返回可重试的系统错误。
             * @param dto 清理参数
             * @return 统一响应结果
             */
            @Override
            public Result<Void> cleanupCheckout(CartCheckoutCleanupDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "购物车服务不可用");
            }
        };
    }
}
