package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.IdFeignClient;

/**
* ID 服务 Feign 降级工厂，在 ID 服务不可用时返回受控降级结果（按当前实现：nextId 返回 null）。
 */
@Slf4j
@Component
public class IdFeignFallbackFactory implements FallbackFactory<IdFeignClient> {
    @Override
    public IdFeignClient create(Throwable cause) {
        log.warn("id-service unreachable, using sentinel id=-1: {}", cause.getMessage());
        return bizTag -> -1L;
    }
}