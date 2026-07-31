package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: HealthControllerV1 description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class HealthControllerV1 {

    /**
     * 健康检查。
     * @return 健康状态
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("platform-ok");
    }
}