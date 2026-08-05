package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台控制器，提供Health RESTful API
 * @Version: 1.1
 * @DateTime: 2026/08/05
 **/
@RestController
@RequestMapping("/api/v1/platform")
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