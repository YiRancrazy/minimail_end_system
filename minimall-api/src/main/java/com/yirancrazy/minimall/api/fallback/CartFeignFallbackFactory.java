package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartFeignFallbackFactory 类型，承载相关业务能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Component
public class CartFeignFallbackFactory implements FallbackFactory<CartFeignClient> {
    /**
     * 创建业务数据。
     * @param cause cause 参数
     * @return 处理结果
     */
    @Override
    public CartFeignClient create(Throwable cause) {
        log.warn("cart-service 不可达，返回哨兵 count=-1: {}", cause.getMessage());
        return new CartFeignClient() {
            /**
             * 统计业务数据。
             * @param userId userId 参数
             * @return 统一响应结果
             */
            @Override
            public Result<Long> countByUser(Long userId) {
                return Result.success(-1L);
            }
        };
    }
}
