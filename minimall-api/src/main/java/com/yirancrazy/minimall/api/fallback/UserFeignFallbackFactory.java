package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.feign.UserFeignClient;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserFeign Feign 降级工厂，处理UserFeign服务调用失败降级
 * @Version: 1.0
 * @DateTime: 2026/07/31
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