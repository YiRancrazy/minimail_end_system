package com.yirancrazy.minimall.auth.service;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;

public interface AuthService {
    TokenVO register(RegisterDTO dto);

    TokenVO login(LoginDTO dto);

    UserInfoVO me(String token);
}