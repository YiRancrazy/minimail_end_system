package com.yirancrazy.minimall.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAuthManager userAuthManager;
    private final JwtUtil jwtUtil;
    private final long ttlSeconds;

    public AuthServiceImpl(UserAuthManager userAuthManager,
                           JwtUtil jwtUtil,
                           @Value("${minimall.jwt.ttl-seconds:86400}") long ttlSeconds) {
        this.userAuthManager = userAuthManager;
        this.jwtUtil = jwtUtil;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public TokenVO register(RegisterDTO dto) {
        UserAuthPO existing = userAuthManager.getOne(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (existing != null) {
            throw new BizException(AuthCodeEnum.USER_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setUsername(dto.getUsername());
        po.setPasswordHash(hash);
        po.setSalt(salt);
        po.setRole("USER");
        po.setStatus(1);
        userAuthManager.save(po);
        return new TokenVO(jwtUtil.sign(po.getId(), po.getUsername(), po.getRole()),
            "Bearer", ttlSeconds);
    }

    @Override
    public TokenVO login(LoginDTO dto) {
        UserAuthPO po = userAuthManager.getOne(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(dto.getPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        return new TokenVO(jwtUtil.sign(po.getId(), po.getUsername(), po.getRole()),
            "Bearer", ttlSeconds);
    }

    @Override
    public UserInfoVO me(String token) {
        Claims c;
        try {
            c = jwtUtil.parse(token);
        } catch (Exception ex) {
            throw new BizException(AuthCodeEnum.TOKEN_INVALID.getCode(),
                AuthCodeEnum.TOKEN_INVALID.getAlias(),
                AuthCodeEnum.TOKEN_INVALID.getMessage());
        }
        return new UserInfoVO(Long.parseLong(c.getSubject()), c.get("username", String.class),
            c.get("role", String.class));
    }
}