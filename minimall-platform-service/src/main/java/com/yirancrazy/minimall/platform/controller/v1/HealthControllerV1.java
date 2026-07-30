package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
* 平台服务健康检查控制器，对外暴露 `/internal/platform/health` 端点用于存活探活。
 */
@RestController
@RequestMapping("/internal/platform")
public class HealthControllerV1 {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("platform-ok");
    }
}