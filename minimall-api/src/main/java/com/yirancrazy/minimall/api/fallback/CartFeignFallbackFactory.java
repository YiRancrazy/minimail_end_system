package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDto;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDto;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 在购物车服务不可用时提供计数降级值和结算快照失败响应.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Component
public class CartFeignFallbackFactory implements FallbackFactory<CartFeignClient> {
    /**
     * 为购物车远程调用异常创建降级客户端.
     *
     * @param cause 购物车远程调用异常
     * @return 购物车降级客户端
     */
    @Override
    public CartFeignClient create(Throwable cause) {
        log.warn("cart-service 不可达: {}", cause.getMessage());
        return new CartFeignClient() {
            /**
             * 购物车服务不可用时返回 -1 作为计数不可用标记.
             *
             * @param userId 用户 ID
             * @return 包含 -1 计数的成功响应
             */
            @Override
            public Result<Long> countByUser(Long userId) {
                return Result.success(-1L);
            }

            /**
             * 购物车服务不可用时返回系统错误，阻止结算继续.
             *
             * @param dto 用户及待结算购物车条目 ID
             * @return 系统错误响应
             */
            @Override
            public Result<List<CartCheckoutItemDto>> getCheckoutItems(CartCheckoutQueryDto dto) {
                return Result.fail(CommonCode.SYS_ERROR, "购物车服务不可用");
            }
        };
    }
}
