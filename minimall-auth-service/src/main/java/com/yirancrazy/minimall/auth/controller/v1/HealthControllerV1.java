package com.yirancrazy.minimall.auth.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证服务的轻量健康检查接口；不依赖数据库、Redis 或远程服务，用于确认进程可响应请求。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class HealthControllerV1 {

    /**
     * 返回认证服务固定的存活标识。
     *
     * @return 成功响应，数据为 {@code auth-ok}
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("auth-ok");
    }
}
