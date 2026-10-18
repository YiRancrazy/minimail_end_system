package com.yirancrazy.minimall.platform.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: HealthControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform")
public class HealthControllerV1 {


    /**
     * 执行 health 方法。
     * @return 统一响应结果
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("platform-ok");
    }
}