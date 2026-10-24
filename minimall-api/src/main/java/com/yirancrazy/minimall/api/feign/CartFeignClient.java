package com.yirancrazy.minimall.api.feign;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutItemDTO;
import com.yirancrazy.minimall.api.dto.cart.CartCheckoutQueryDTO;
import com.yirancrazy.minimall.api.fallback.CartFeignFallbackFactory;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@FeignClient(name = "minimall-cart-service", fallbackFactory = CartFeignFallbackFactory.class)
public interface CartFeignClient {

    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/cart/count")
    Result<Long> countByUser(@RequestParam("userId") Long userId);


    /**
     * 查询购物车结算快照。
     * @param dto 查询参数
     * @return 购物车条目快照
     */
    @PostMapping("/internal/cart/checkout-snapshot")
    Result<List<CartCheckoutItemDTO>> getCheckoutItems(@RequestBody CartCheckoutQueryDTO dto);
}
