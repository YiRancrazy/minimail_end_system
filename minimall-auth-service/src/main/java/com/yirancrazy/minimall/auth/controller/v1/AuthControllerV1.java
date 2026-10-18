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
 * @Description: AuthControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/auth")
public class AuthControllerV1 {

    private final AuthService authService;

    /**
     * 构造 AuthControllerV1 实例。
     * @param authService authService 参数
     */
    public AuthControllerV1(AuthService authService) {
        this.authService = authService;
    }


    /**
     * 执行 register 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }


    /**
     * 执行 login 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }


    /**
     * 执行 refreshToken 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/token/refresh")
    public Result<TokenVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refreshToken(dto.getRefreshToken()));
    }


    /**
     * 执行 logout 方法。
     * @param userId userId 参数
     * @param jti jti 参数
     * @return 统一响应结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long userId,
                               @RequestHeader("X-User-Jti") String jti) {
        authService.signOut(userId, jti);
        return Result.success();
    }


    /**
     * 执行 me 方法。
     * @param authorization authorization 参数
     * @return 统一响应结果
     */
    @GetMapping("/me")
    public Result<UserInfoVO> me(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith("Bearer ")
            ? authorization.substring(7) : authorization;
        return Result.success(authService.me(token));
    }


    /**
     * 执行 changePassword 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long userId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(userId, dto);
        return Result.success();
    }


    /**
     * 发送业务消息。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/password/reset/request")
    public Result<Void> sendResetCode(@Valid @RequestBody SendResetCodeDTO dto) {
        authService.sendResetCode(dto);
        return Result.success();
    }


    /**
     * 执行 resetPassword 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/password/reset/confirm")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        authService.resetPassword(dto);
        return Result.success();
    }
}