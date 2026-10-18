package com.yirancrazy.minimall.api.feign;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartFeignClient 接口，定义相关能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface CartFeignClient {

    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/internal/cart/count")
    Result<Long> countByUser(@RequestParam("userId") Long userId);
}
