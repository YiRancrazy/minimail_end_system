package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.IdFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IdFeignFallbackFactory implements FallbackFactory<IdFeignClient> {
    @Override
    public IdFeignClient create(Throwable cause) {
        log.warn("id-service unreachable, using sentinel id=-1: {}", cause.getMessage());
        return bizTag -> -1L;
    }
}