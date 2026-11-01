package com.yirancrazy.minimall.auth.controller.v1;

import com.yirancrazy.minimall.api.dto.auth.RefreshTokenDto;
import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDto;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDto;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 普通用户认证 HTTP 边界；提供注册、登录、令牌刷新、退出、资料查询和密码重置接口。
 * 身份头由网关传入，业务异常统一交由全局异常处理器转换为 Result。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/auth")
public class AuthControllerV1 {

    private final AuthService authService;

    /**
     * 注入普通用户认证服务。
     *
     * @param authService 普通用户认证业务入口
     */
    public AuthControllerV1(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册普通用户账号，并在成功后返回访问令牌和刷新令牌。
     *
     * @param dto 注册账号和密码
     * @return 成功响应，数据为签发令牌
     */
    @PostMapping("/register")
    public Result<TokenVo> register(@Valid @RequestBody RegisterDto dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 校验普通用户凭据并签发访问令牌和刷新令牌。
     *
     * @param dto 登录账号和密码
     * @return 成功响应，数据为签发令牌
     */
    @PostMapping("/login")
    public Result<TokenVo> login(@Valid @RequestBody LoginDto dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 使用刷新令牌换取新的访问令牌和刷新令牌。
     *
     * @param dto 包含刷新令牌的请求体
     * @return 成功响应，数据为轮换后的令牌
     */
    @PostMapping("/token/refresh")
    public Result<TokenVo> refreshToken(@Valid @RequestBody RefreshTokenDto dto) {
        return Result.success(authService.refreshToken(dto.getRefreshToken()));
    }

    /**
     * 退出当前普通用户会话，清除刷新令牌并撤销当前 JWT。
     *
     * @param userId 网关注入的认证用户 ID
     * @param jti 网关注入的当前 JWT 唯一标识
     * @return 成功响应
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader("X-Auth-User-Id") Long userId, @RequestHeader("X-User-Jti") String jti) {
        authService.signOut(userId, jti);
        return Result.success();
    }

    /**
     * 查询当前普通用户信息；Authorization 可带或不带 Bearer 前缀。
     *
     * @param authorization 当前请求的 JWT 或 Bearer JWT
     * @return 成功响应，数据为 JWT 中的安全用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoVo> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
        return Result.success(authService.me(token));
    }

    /**
     * 修改当前普通用户密码，并使该账号已有刷新令牌失效。
     *
     * @param userId 网关注入的认证用户 ID
     * @param dto 旧密码和新密码
     * @return 成功响应
     */
    @PutMapping("/password")
    public Result<Void> changePassword(
            @RequestHeader("X-Auth-User-Id") Long userId, @Valid @RequestBody ChangePasswordDto dto) {
        authService.changePassword(userId, dto);
        return Result.success();
    }

    /**
     * 请求生成密码重置验证码；当前实现保存验证码但未调用真实短信通道。
     *
     * @param dto 待重置密码的账号
     * @return 成功响应
     */
    @PostMapping("/password/reset/request")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeDto dto) {
        authService.sendResetCode(dto);
        return Result.success();
    }

    /**
     * 使用验证码确认身份并重置普通用户密码。
     *
     * @param dto 账号、6 位验证码和新密码
     * @return 成功响应
     */
    @PostMapping("/password/reset/confirm")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        authService.resetPassword(dto);
        return Result.success();
    }
}
