package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface IdFeignClient {
    @GetMapping("/internal/id/next")
    Long nextId(@RequestParam("bizTag") String bizTag);
}