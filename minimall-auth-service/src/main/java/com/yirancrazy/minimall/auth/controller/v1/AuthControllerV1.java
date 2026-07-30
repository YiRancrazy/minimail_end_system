package com.yirancrazy.minimall.auth.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.auth.RefreshTokenDTO;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.result.Result;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthControllerV1 {

    private final AuthService authService;

    public AuthControllerV1(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/refresh-token")
    public Result<TokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refreshToken(dto.getRefreshToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId,
                               @RequestHeader("X-User-Jti") String jti) {
        authService.signOut(userId, jti);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ")
            ? authorization.substring(7) : authorization;
        return Result.success(authService.me(token));
    }
}