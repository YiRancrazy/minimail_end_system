package com.yirancrazy.minimall.auth.controller.v1;

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
import com.yirancrazy.minimall.common.result.Result;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端认证控制器，提供商家登录、登出、修改密码接口。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@RestController
@RequestMapping("/api/v1/merchant/auth")
public class MerchantAuthControllerV1 {

    private final MerchantAuthService merchantAuthService;

    public MerchantAuthControllerV1(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }

    /**
     * 商家登录。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(merchantAuthService.login(dto));
    }

    /**
     * 商家注册，创建 role=MERCHANT 的账号并签发令牌。
     * @param dto 注册DTO
     * @return 令牌VO
     */
    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(merchantAuthService.register(dto));
    }

    /**
     * 商家登出。
     * @param merchantAccountId 商家账号ID（来自网关 X-User-Id）
     * @param jti 令牌唯一标识（来自网关 X-User-Jti）
     * @return 无返回值
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-User-Id") Long merchantAccountId,
                               @RequestHeader("X-User-Jti") String jti) {
        merchantAuthService.signOut(merchantAccountId, jti);
        return Result.success();
    }

    /**
     * 商家修改密码，需登录态校验旧密码。
     * @param merchantAccountId 商家账号ID（来自网关 X-User-Id）
     * @param dto 修改密码DTO
     * @return 无返回值
     */
    @PostMapping("/password/change")
    public Result<Void> changePassword(@RequestHeader("X-User-Id") Long merchantAccountId,
                                       @Valid @RequestBody ChangePasswordDTO dto) {
        merchantAuthService.changePassword(merchantAccountId, dto);
        return Result.success();
    }
}
