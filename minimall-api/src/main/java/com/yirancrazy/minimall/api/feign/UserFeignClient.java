package com.yirancrazy.minimall.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.api.fallback.UserFeignFallbackFactory;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserFeignClient description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface UserFeignClient {
    @GetMapping("/internal/user/{id}")
    UserSnapshotDTO snapshot(@PathVariable("id") Long id);
}