package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.fallback.IdFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "minimall-id-service", fallbackFactory = IdFeignFallbackFactory.class)
public interface IdFeignClient {
    @GetMapping("/internal/id/next")
    Long nextId(@RequestParam("bizTag") String bizTag);
}