package com.yirancrazy.minimall.auth.controller.v1;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.service.MerchantAuthService;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.result.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家认证 HTTP 边界；提供商家注册、登录、退出、修改密码和当前账号查询接口。
 * 控制器只做请求适配，身份头由网关传入，认证规则由 MerchantAuthService 承担。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/auth")
public class MerchantAuthControllerV1 {

  private final MerchantAuthService merchantAuthService;

  /**
   * 注入商家认证服务。
   *
   * @param merchantAuthService 商家认证业务入口
   */
  public MerchantAuthControllerV1(MerchantAuthService merchantAuthService) {
    this.merchantAuthService = merchantAuthService;
  }

  /**
   * 校验商家账号类型、状态和密码后签发令牌。
   *
   * @param dto 商家登录账号和密码
   * @return 成功响应，数据为签发令牌
   */
  @PostMapping("/login")
  public Result<TokenVo> login(@Valid @RequestBody LoginDto dto) {
    return Result.success(merchantAuthService.login(dto));
  }

  /**
   * 注册商家账号；账号已存在时由服务层拒绝。
   *
   * @param dto 商家注册账号和密码
   * @return 成功响应，数据为签发令牌
   */
  @PostMapping("/register")
  public Result<TokenVo> register(@Valid @RequestBody RegisterDto dto) {
    return Result.success(merchantAuthService.register(dto));
  }

  /**
   * 退出当前商家会话，并撤销当前 JWT。
   *
   * @param merchantAccountId 网关注入的商家认证账号 ID
   * @param jti 网关注入的当前 JWT 唯一标识
   * @return 成功响应
   */
  @PostMapping("/logout")
  public Result<Void> logout(
      @RequestHeader("X-Auth-User-Id") Long merchantAccountId,
      @RequestHeader("X-User-Jti") String jti) {
    merchantAuthService.signOut(merchantAccountId, jti);
    return Result.success();
  }

  /**
   * 修改当前商家密码，并清除该账号的刷新令牌。
   *
   * @param merchantAccountId 网关注入的商家认证账号 ID
   * @param dto 旧密码和新密码
   * @return 成功响应
   */
  @PostMapping("/password/change")
  public Result<Void> changePassword(
      @RequestHeader("X-Auth-User-Id") Long merchantAccountId,
      @Valid @RequestBody ChangePasswordDto dto) {
    merchantAuthService.changePassword(merchantAccountId, dto);
    return Result.success();
  }

  /**
   * 查询当前商家信息；Authorization 可带或不带 Bearer 前缀。
   *
   * @param authorization 当前请求的 JWT 或 Bearer JWT
   * @return 成功响应，数据为 JWT 中的安全用户信息
   */
  @GetMapping("/me")
  public Result<UserInfoVo> me(@RequestHeader("Authorization") String authorization) {
    String token = authorization.startsWith("Bearer ") ? authorization.substring(7) : authorization;
    return Result.success(merchantAuthService.me(token));
  }
}
