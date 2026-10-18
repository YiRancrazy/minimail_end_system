package com.yirancrazy.minimall.auth.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.service.MerchantAuthService;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.result.Result;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantAuthControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/auth")
public class MerchantAuthControllerV1 {

    private final MerchantAuthService merchantAuthService;

    /**
     * 构造 MerchantAuthControllerV1 实例。
     * @param merchantAuthService merchantAuthService 参数
     */
    public MerchantAuthControllerV1(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }


    /**
     * 执行 login 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(merchantAuthService.login(dto));
    }


    /**
     * 执行 register 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(merchantAuthService.register(dto));
    }


    /**
     * 执行 logout 方法。
     * @param merchantAccountId merchantAccountId 参数
     * @param jti jti 参数
     * @return 统一响应结果
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long merchantAccountId,
                               @RequestHeader("X-User-Jti") String jti) {
        merchantAuthService.signOut(merchantAccountId, jti);
        return Result.success();
    }


    /**
     * 执行 changePassword 方法。
     * @param merchantAccountId merchantAccountId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/password/change")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long merchantAccountId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        merchantAuthService.changePassword(merchantAccountId, dto);
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
        return Result.success(merchantAuthService.me(token));
    }
}
