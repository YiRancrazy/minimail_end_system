package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证领域服务接口，定义注册、登录与当前用户查询契约。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
public interface AuthService {
    TokenVO register(RegisterDTO dto);

    TokenVO login(LoginDTO dto);

    UserInfoVO me(String token);
}