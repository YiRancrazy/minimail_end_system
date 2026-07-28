package com.yirancrazy.minimall.auth.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class HealthControllerV1 {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("auth-ok");
    }
}