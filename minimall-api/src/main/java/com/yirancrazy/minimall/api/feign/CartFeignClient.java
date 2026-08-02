package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Cart Feign 客户端，调用Cart服务接口
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
public interface CartFeignClient {
    /**
     * 统计指定用户购物车条目数量。
     * @param userId 用户ID
     * @return 购物车条目数量；服务不可用时由 fallback 返回 -1
     */
    @GetMapping("/internal/cart/count")
    Result<Long> countByUser(@RequestParam("userId") Long userId);
}
