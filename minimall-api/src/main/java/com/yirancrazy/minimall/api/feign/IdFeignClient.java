package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID 服务 Feign 客户端，提供跨服务号段下发能力。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@FeignClient(value = "minimall-id-service", fallbackFactory = IdFeignFallbackFactory.class)
public interface IdFeignClient {
    @GetMapping("/internal/id/next")
    Long nextId(@RequestParam("bizTag") String bizTag);
}