package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.IdFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: IdFeignFallbackFactory description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class IdFeignFallbackFactory implements FallbackFactory<IdFeignClient> {
    @Override
    public IdFeignClient create(Throwable cause) {
        log.warn("id-service unreachable, using sentinel id=-1: {}", cause.getMessage());
        return bizTag -> -1L;
    }
}