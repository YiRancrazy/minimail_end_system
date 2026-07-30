package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.feign.UserFeignClient;

/**
* 用户服务 Feign 降级工厂，在用户服务不可用时返回受控降级结果。
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("user-service unreachable, returning sentinel snapshot: {}", cause.getMessage());
        return id -> new UserSnapshotDTO(-1L, "unknown", "ANONYMOUS");
    }
}