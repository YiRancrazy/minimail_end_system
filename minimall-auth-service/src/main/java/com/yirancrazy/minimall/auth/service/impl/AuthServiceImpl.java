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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 认证领域服务实现，负责用户注册、密码校验与访问令牌签发。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
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

    /**
     * 注册新用户：校验用户名唯一后生成随机盐，使用 BCrypt 加盐哈希密码入库，并签发访问令牌。
     *
     * @param dto 注册入参，包含 username 与 password
     * @return 包含 JWT 访问令牌、Token 类型与过期秒数的视图
     */
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
        String accessToken = jwtUtil.sign(po.getId(), po.getUsername(), po.getRole());
        return new TokenVO(accessToken, null, "Bearer", ttlSeconds);
    }

    /**
     * 用户登录：按用户名查找账号并校验加盐哈希密码，成功后签发新的访问令牌。
     *
     * @param dto 登录入参，包含 username 与 password
     * @return 包含 JWT 访问令牌、Token 类型与过期秒数的视图
     */
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
        String accessToken = jwtUtil.sign(po.getId(), po.getUsername(), po.getRole());
        return new TokenVO(accessToken, null, "Bearer", ttlSeconds);
    }

    /**
     * 解析 JWT 令牌并返回当前登录用户的基本信息（用户ID、用户名、角色）。
     *
     * @param token JWT 访问令牌字符串
     * @return 包含 userId / username / role 的当前用户视图
     */
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