package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.feign.IdFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ID 服务 Feign 降级工厂，在 ID 服务不可用时返回受控降级结果（按当前实现：nextId 返回 null）。
 * @Version: 1.0
 * @DateTime: 2026/7/29
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