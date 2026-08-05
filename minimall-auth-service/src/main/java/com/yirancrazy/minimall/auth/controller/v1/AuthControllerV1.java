package com.yirancrazy.minimall.auth.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.auth.RefreshTokenDTO;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDTO;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDTO;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthControllerV1 类。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@RestController
@RequestMapping("/api/v1/user/auth")
public class AuthControllerV1 {

    private final AuthService authService;

    public AuthControllerV1(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册。
     * @param dto 注册DTO
     * @return 令牌VO
     */
    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 用户登录。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 刷新令牌。
     * @param dto 刷新令牌DTO
     * @return 新的令牌VO
     */
    @PostMapping("/token/refresh")
    public Result<TokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refreshToken(dto.getRefreshToken()));
    }

    /**
     * 用户登出。
     * @param userId 用户ID
     * @param jti 令牌唯一标识
     * @return 无返回值
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId,
                               @RequestHeader("X-User-Jti") String jti) {
        authService.signOut(userId, jti);
        return Result.success();
    }

    /**
     * 获取当前用户信息。
     * @param authorization 授权头
     * @return 用户信息VO
     */
    @GetMapping("/me")
    public Result<UserInfoVO> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ")
            ? authorization.substring(7) : authorization;
        return Result.success(authService.me(token));
    }

    /**
     * 修改密码，需登录态校验旧密码。
     * @param userId 用户ID
     * @param dto 修改密码DTO
     * @return 无返回值
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(userId, dto);
        return Result.success();
    }

    /**
     * 发送重置密码验证码。
     * @param dto 发送验证码DTO
     * @return 无返回值
     */
    @PostMapping("/password/reset/request")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeDTO dto) {
        authService.sendResetCode(dto);
        return Result.success();
    }

    /**
     * 重置密码，校验验证码后设置新密码。
     * @param dto 重置密码DTO
     * @return 无返回值
     */
    @PostMapping("/password/reset/confirm")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }
}