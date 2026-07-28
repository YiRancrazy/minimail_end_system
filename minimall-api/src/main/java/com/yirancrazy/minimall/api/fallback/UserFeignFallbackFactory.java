package com.yirancrazy.minimall.api.fallback;

import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("user-service unreachable, returning sentinel snapshot: {}", cause.getMessage());
        return id -> new UserSnapshotDTO(-1L, "unknown", "ANONYMOUS");
    }
}