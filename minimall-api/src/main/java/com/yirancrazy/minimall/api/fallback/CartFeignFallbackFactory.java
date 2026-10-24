package com.yirancrazy.minimall.api.fallback;

import java.util.List;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDTO;
import com.yirancrazy.minimall.api.feign.CartFeignClient;
import com.yirancrazy.minimall.common.result.CommonCode;
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
        log.warn("cart-service 不可达: {}", cause.getMessage());
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

            /**
             * 返回不可用的快照错误。
             * @param dto 查询参数
             * @return 统一响应结果
             */
            @Override
            public Result<List<CartCheckoutItemDTO>> getCheckoutItems(CartCheckoutQueryDTO dto) {
                return Result.fail(CommonCode.SYS_ERROR, "购物车服务不可用");
            }
        };
    }
}
