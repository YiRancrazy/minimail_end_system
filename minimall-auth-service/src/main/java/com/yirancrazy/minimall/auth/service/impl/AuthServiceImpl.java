package com.yirancrazy.minimall.auth.service.impl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDTO;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务实现，处理核心业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";
    private static final String RESET_CODE_KEY_PREFIX = "pwd:reset:code:";
    private static final long RESET_CODE_TTL_SECONDS = 600L;

    private final UserAuthManager userAuthManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final long refreshTtlSeconds;

    public AuthServiceImpl(UserAuthManager userAuthManager,
                           JwtUtil jwtUtil,
                           StringRedisTemplate redisTemplate,
                           @Value("${minimall.jwt.ttl-seconds:900}") long ttlSeconds,
                           @Value("${minimall.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.userAuthManager = userAuthManager;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    /**
     * 用户注册。
     * @param dto 注册DTO
     * @return 令牌VO
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
        return issueTokens(po.getId(), po.getUsername(), po.getRole());
    }

    /**
     * 用户登录。
     * @param dto 登录DTO
     * @return 令牌VO
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
        return issueTokens(po.getId(), po.getUsername(), po.getRole());
    }

    /**
     * 刷新令牌。
     * @param refreshToken 刷新令牌
     * @return 新的令牌VO
     */
    @Override
    public TokenVO refreshToken(String refreshToken) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            throw new BizException(AuthCodeEnum.REFRESH_TOKEN_INVALID);
        }
        String matchedKey = null;
        for (String key : keys) {
            String stored = redisTemplate.opsForValue().get(key);
            if (refreshToken.equals(stored)) {
                matchedKey = key;
                break;
            }
        }
        if (matchedKey == null) {
            throw new BizException(AuthCodeEnum.REFRESH_TOKEN_INVALID);
        }
        redisTemplate.delete(matchedKey);
        String[] parts = matchedKey.split(":");
        Long userId = Long.parseLong(parts[1]);
        String oldJti = parts[2];
        redisTemplate.opsForValue().set(
            BLACKLIST_KEY_PREFIX + oldJti, "1", ttlSeconds, TimeUnit.SECONDS);

        UserAuthPO po = userAuthManager.getById(userId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        log.info("refresh token rotated, userId={}", userId);
        return issueTokens(po.getId(), po.getUsername(), po.getRole());
    }

    /**
     * 用户登出。
     * @param userId 用户ID
     * @param jti 令牌唯一标识
     */
    @Override
    public void signOut(Long userId, String jti) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + userId + ":*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        redisTemplate.opsForValue().set(
            BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        log.info("user signed out, userId={}", userId);
    }

    /**
     * 获取当前用户信息。
     * @param token 令牌
     * @return 用户信息VO
     */
    @Override
    public UserInfoVO me(String token) {
        Claims c;
        try {
            c = jwtUtil.parse(token);
        }
        catch (JwtException ex) {
            throw new BizException(AuthCodeEnum.TOKEN_INVALID.getCode(),
                AuthCodeEnum.TOKEN_INVALID.getAlias(),
                AuthCodeEnum.TOKEN_INVALID.getMessage());
        }
        return new UserInfoVO(Long.parseLong(c.getSubject()), c.get("username", String.class),
            c.get("role", String.class));
    }

    /**
     * 修改密码，需校验旧密码，成功后失效已有刷新令牌。
     * @param userId 用户ID
     * @param dto 修改密码DTO
     */
    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        UserAuthPO po = userAuthManager.getById(userId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(dto.getOldPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        applyNewPassword(po, dto.getNewPassword());
        invalidateRefreshTokens(userId);
        log.info("password changed, userId={}", userId);
    }

    /**
     * 发送重置密码验证码，首期短信通道未接入时验证码通过日志输出用于联调。
     * @param dto 发送验证码DTO
     */
    @Override
    public void sendResetCode(SendResetCodeDTO dto) {
        UserAuthPO po = userAuthManager.getOne(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        String code = String.format("%06d",
            java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set(
            RESET_CODE_KEY_PREFIX + dto.getUsername(), code, RESET_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        // SMS channel not ready in first iteration; log code for integration debugging.
        log.info("reset code generated, username={}, code={}", dto.getUsername(), code);
    }

    /**
     * 重置密码，校验验证码后设置新密码，成功后失效已有刷新令牌。
     * @param dto 重置密码DTO
     */
    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        UserAuthPO po = userAuthManager.getOne(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        String stored = redisTemplate.opsForValue().get(RESET_CODE_KEY_PREFIX + dto.getUsername());
        if (stored == null || !stored.equals(dto.getVerifyCode())) {
            throw new BizException(AuthCodeEnum.VERIFY_CODE_INVALID);
        }
        applyNewPassword(po, dto.getNewPassword());
        redisTemplate.delete(RESET_CODE_KEY_PREFIX + dto.getUsername());
        invalidateRefreshTokens(po.getId());
        log.info("password reset, userId={}", po.getId());
    }

    private void applyNewPassword(UserAuthPO po, String newPassword) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt());
        po.setPasswordHash(hash);
        po.setSalt(salt);
        userAuthManager.updateById(po);
    }

    private void invalidateRefreshTokens(Long userId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private TokenVO issueTokens(Long userId, String username, String role) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtUtil.sign(userId, username, role, jti);
        String refreshToken = jwtUtil.generateRefreshToken();
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + userId + ":" + jti,
            refreshToken, refreshTtlSeconds, TimeUnit.SECONDS);
        return new TokenVO(accessToken, refreshToken, "Bearer", ttlSeconds);
    }
}