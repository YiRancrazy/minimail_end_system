package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家认证契约；限定商家账号的注册、登录、退出、密码修改和当前身份查询。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface MerchantAuthService {

    /**
     * 注册商家账号并签发令牌；创建账号时固定使用商家类型和默认商家角色。
     *
     * @param dto 商家注册账号和密码
     * @return 新签发的令牌
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号已存在时抛出
     */
    TokenVo register(RegisterDto dto);

    /**
     * 校验商家账号类型、启用状态和密码，更新最近登录时间后签发令牌。
     *
     * @param dto 商家登录账号和密码
     * @return 新签发的令牌
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在、类型不匹配、禁用或密码错误时抛出
     */
    TokenVo login(LoginDto dto);

    /**
     * 退出商家会话，清除刷新令牌并撤销当前 JWT。
     *
     * @param merchantAccountId 商家认证账号 ID
     * @param jti 当前 JWT 唯一标识
     */
    void signOut(Long merchantAccountId, String jti);

    /**
     * 校验旧密码后更新商家密码，并使该账号的刷新令牌全部失效。
     *
     * @param merchantAccountId 商家认证账号 ID
     * @param dto 旧密码和新密码
     * @throws com.yirancrazy.minimall.common.exception.BizException 账号不存在或旧密码错误时抛出
     */
    void changePassword(Long merchantAccountId, ChangePasswordDto dto);

    /**
     * 校验并解析 JWT，返回当前商家账号的安全身份信息。
     *
     * @param token 不带 Bearer 前缀的 JWT
     * @return 商家认证账号 ID、账号和角色信息
     * @throws com.yirancrazy.minimall.common.exception.BizException JWT 无效或过期时抛出
     */
    UserInfoVo me(String token);
}
