package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.fallback.NotifyFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
* 通知服务 Feign 客户端，提供跨服务通知发送能力。
 */
@FeignClient(value = "minimall-notify-service", fallbackFactory = NotifyFeignFallbackFactory.class)
public interface NotifyFeignClient {
    @PostMapping("/internal/notify/push")
    Boolean push(@RequestBody NotifyEventDTO dto);
}