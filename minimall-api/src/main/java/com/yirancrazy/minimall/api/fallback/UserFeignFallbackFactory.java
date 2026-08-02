package com.yirancrazy.minimall.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.feign.UserFeignClient;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserFeign Feign 降级工厂，处理UserFeign服务调用失败降级
 * @Version: 1.1
 * @DateTime: 2026/08/02
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {
    @Override
    public UserFeignClient create(Throwable cause) {
        log.warn("user-service unreachable, returning sentinel snapshot: {}", cause.getMessage());
        return new UserFeignClient() {
            @Override
            public Result<UserSnapshotDTO> snapshot(Long id) {
                return Result.success(new UserSnapshotDTO(-1L, "unknown", "ANONYMOUS"));
            }
        };
    }
}
