package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.notify.NotifyEventDTO;
import com.yirancrazy.minimall.api.feign.NotifyFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotifyFeignFallbackFactory implements FallbackFactory<NotifyFeignClient> {
    @Override
    public NotifyFeignClient create(Throwable cause) {
        log.warn("notify-service unreachable: {}", cause.getMessage());
        return (NotifyEventDTO dto) -> false;
    }
}