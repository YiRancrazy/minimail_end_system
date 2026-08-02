package com.yirancrazy.minimall.auth.controller.v1;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端认证控制器，提供平台管理员登录、登出接口。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@RestController
@RequestMapping("/api/v1/platform/auth")
public class PlatformAuthControllerV1 {

    private final PlatformAuthService platformAuthService;

    public PlatformAuthControllerV1(PlatformAuthService platformAuthService) {
        this.platformAuthService = platformAuthService;
    }

    /**
     * 平台管理员登录。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(platformAuthService.login(dto));
    }

    /**
     * 平台管理员登出。
     * @param adminAccountId 管理员账号ID（来自网关 X-User-Id）
     * @param jti 令牌唯一标识（来自网关 X-User-Jti）
     * @return 无返回值
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long adminAccountId,
                               @RequestHeader("X-User-Jti") String jti) {
        platformAuthService.signOut(adminAccountId, jti);
        return Result.success();
    }
}
