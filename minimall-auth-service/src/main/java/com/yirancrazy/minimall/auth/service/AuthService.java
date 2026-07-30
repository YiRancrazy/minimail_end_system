package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务接口，定义核心业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
public interface AuthService {
    /**
     * 用户注册。
     * @param dto 注册DTO
     * @return 令牌VO
     */
    TokenVO register(RegisterDTO dto);

    /**
     * 用户登录。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    TokenVO login(LoginDTO dto);

    /**
     * 刷新令牌。
     * @param refreshToken 刷新令牌
     * @return 新的令牌VO
     */
    TokenVO refreshToken(String refreshToken);

    /**
     * 用户登出。
     * @param userId 用户ID
     * @param jti 令牌唯一标识
     */
    void signOut(Long userId, String jti);

    /**
     * 获取当前用户信息。
     * @param token 令牌
     * @return 用户信息VO
     */
    UserInfoVO me(String token);
}