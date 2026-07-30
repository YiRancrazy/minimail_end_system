package com.yirancrazy.minimall.api.feign;

import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.UserFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
* 用户服务 Feign 客户端，提供跨服务用户快照查询能力。
 */
@FeignClient(value = "minimall-user-service", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {
    @GetMapping("/internal/user/{id}")
    UserSnapshotDTO snapshot(@PathVariable("id") Long id);
}