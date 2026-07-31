package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.fallback.NotifyFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: NotifyFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface NotifyFeignClient {
    @PostMapping("/internal/notify/push")
    Boolean push(@RequestBody NotifyEventDTO dto);
}