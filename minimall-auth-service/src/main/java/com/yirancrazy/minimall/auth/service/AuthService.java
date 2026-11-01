package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDto;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDto;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 普通用户认证契约；定义注册、登录、令牌轮换、退出、身份查询和密码重置流程。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface AuthService {

    /**
     * 注册唯一账号，创建关联业务用户档案并签发初始令牌。
     *
     * @param dto 注册账号和密码
     * @return 访问令牌、刷新令牌和令牌类型
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号已存在或远程用户创建失败时抛出
     */
    TokenVo register(RegisterDto dto);

    /**
     * 校验普通用户账号类型、启用状态和密码，成功后更新最近登录时间并签发令牌。
     *
     * @param dto 登录账号和密码
     * @return 新签发的令牌
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在、类型不匹配、禁用或密码错误时抛出
     */
    TokenVo login(LoginDto dto);

    /**
     * 原子消费刷新令牌并轮换为一组新令牌；旧刷新令牌不能再次使用。
     *
     * @param refreshToken 已签名的刷新 JWT
     * @return 轮换后的访问令牌和刷新令牌
     * @throws com.yirancrazy.minimall.common.exception.BizException 刷新令牌无效、已消费或用户不存在时抛出
     */
    TokenVo refreshToken(String refreshToken);

    /**
     * 退出指定普通用户会话，清除其刷新令牌并撤销当前访问令牌。
     *
     * @param userId 认证账号 ID
     * @param jti 当前 JWT 唯一标识
     */
    void signOut(Long userId, String jti);

    /**
     * 校验并解析 JWT，返回当前普通用户的安全身份信息。
     *
     * @param token 不带 Bearer 前缀的 JWT
     * @return 认证账号 ID、业务用户 ID、账号和角色信息
     * @throws com.yirancrazy.minimall.common.exception.BizException JWT 无效或过期时抛出
     */
    UserInfoVo me(String token);

    /**
     * 校验旧密码后设置加盐新密码，并使该账号的刷新令牌全部失效。
     *
     * @param userId 认证账号 ID
     * @param dto 旧密码和新密码
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在或旧密码错误时抛出
     */
    void changePassword(Long userId, ChangePasswordDto dto);

    /**
     * 生成并缓存密码重置验证码；同一账号 60 秒内只允许申请一次。
     *
     * @param dto 目标账号
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在或仍在冷却期时抛出
     */
    void sendResetCode(SendResetCodeDto dto);

    /**
     * 校验重置验证码并设置新密码，同时清除验证码和该账号的刷新令牌。
     *
     * @param dto 账号、6 位验证码和新密码
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在、验证码错误或尝试次数超限时抛出
     */
    void resetPassword(ResetPasswordDto dto);
}
