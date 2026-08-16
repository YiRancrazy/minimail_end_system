package com.yirancrazy.minimall.auth.service.impl;

import java.time.LocalDateTime;
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
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPO;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: 业务服务实现，处理核心业务逻辑。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final int BCRYPT_COST = 12;
    private static final int ACCOUNT_TYPE_USER = 1;
    private static final long DEFAULT_ROLE_ID_USER = 1L;

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";
    private static final String RESET_CODE_KEY_PREFIX = "pwd:reset:code:";
    private static final long RESET_CODE_TTL_SECONDS = 600L;
    private static final String RESET_CODE_COOLDOWN_KEY_PREFIX = "pwd:reset:cooldown:";
    private static final long RESET_CODE_COOLDOWN_SECONDS = 60L;
    private static final String RESET_CODE_FAIL_KEY_PREFIX = "pwd:reset:fail:";
    private static final long RESET_CODE_FAIL_TTL_SECONDS = 600L;
    private static final long RESET_CODE_FAIL_THRESHOLD = 5L;

    private final AuthUserManager authUserManager;
    private final AuthRoleManager authRoleManager;
    private final AuthTokenBlacklistManager tokenBlacklistManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final long refreshTtlSeconds;

    public AuthServiceImpl(AuthUserManager authUserManager,
                           AuthRoleManager authRoleManager,
                           AuthTokenBlacklistManager tokenBlacklistManager,
                           JwtUtil jwtUtil,
                           StringRedisTemplate redisTemplate,
                           @Value("${minimall.jwt.ttl-seconds:900}") long ttlSeconds,
                           @Value("${minimall.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds) {
        this.authUserManager = authUserManager;
        this.authRoleManager = authRoleManager;
        this.tokenBlacklistManager = tokenBlacklistManager;
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
        AuthUserPO existing = authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (existing != null) {
            throw new BizException(AuthCodeEnum.USER_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt(BCRYPT_COST));
        AuthUserPO po = new AuthUserPO();
        po.setAccount(dto.getAccount());
        po.setPasswordHash(hash);
        po.setSalt(salt);
        po.setAccountType(ACCOUNT_TYPE_USER);
        po.setRoleId(DEFAULT_ROLE_ID_USER);
        po.setStatus(1);
        authUserManager.save(po);
        String roleCode = resolveRoleCode(po.getRoleId());
        return issueTokens(po.getId(), po.getAccount(), roleCode, po.getRoleId());
    }

    /**
     * 用户登录。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @Override
    public TokenVO login(LoginDTO dto) {
        AuthUserPO po = authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        if (po.getAccountType() == null || po.getAccountType() != ACCOUNT_TYPE_USER) {
            throw new BizException(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH);
        }
        if (po.getStatus() == null || po.getStatus() != 1) {
            throw new BizException(AuthCodeEnum.ACCOUNT_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        updateLastLoginAt(po);
        String roleCode = resolveRoleCode(po.getRoleId());
        return issueTokens(po.getId(), po.getAccount(), roleCode, po.getRoleId());
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
        revokeToken(oldJti, userId, "refresh_rotated");

        AuthUserPO po = authUserManager.getById(userId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        log.info("refresh token rotated, userId={}", userId);
        String roleCode = resolveRoleCode(po.getRoleId());
        return issueTokens(po.getId(), po.getAccount(), roleCode, po.getRoleId());
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
        revokeToken(jti, userId, "sign_out");
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
        return new UserInfoVO(Long.parseLong(c.getSubject()),
            c.get("account", String.class),
            c.get("role", String.class),
            c.get("roleId", Long.class));
    }

    /**
     * 修改密码，需校验旧密码，成功后失效已有刷新令牌。
     * @param userId 用户ID
     * @param dto 修改密码DTO
     */
    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        AuthUserPO po = authUserManager.getById(userId);
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
        AuthUserPO po = authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        // setIfAbsent 原子占位冷却键，防止对同一账号轰炸验证码请求
        String cooldownKey = RESET_CODE_COOLDOWN_KEY_PREFIX + dto.getAccount();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
            cooldownKey, "1", RESET_CODE_COOLDOWN_SECONDS, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BizException(AuthCodeEnum.RESET_CODE_TOO_FREQUENT);
        }
        String code = String.format("%06d",
            java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000));
        redisTemplate.opsForValue().set(
            RESET_CODE_KEY_PREFIX + dto.getAccount(), code, RESET_CODE_TTL_SECONDS, TimeUnit.SECONDS);
        // SMS channel not ready in first iteration; log code for integration debugging.
        log.info("reset code generated, account={}", dto.getAccount());
    }

    /**
     * 重置密码，校验验证码后设置新密码，成功后失效已有刷新令牌。
     * @param dto 重置密码DTO
     */
    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        AuthUserPO po = authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        String failKey = RESET_CODE_FAIL_KEY_PREFIX + dto.getAccount();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Long.parseLong(failCount) >= RESET_CODE_FAIL_THRESHOLD) {
            throw new BizException(AuthCodeEnum.VERIFY_CODE_ATTEMPT_EXCEEDED);
        }
        String stored = redisTemplate.opsForValue().get(RESET_CODE_KEY_PREFIX + dto.getAccount());
        if (stored == null || !stored.equals(dto.getVerifyCode())) {
            // 验证码错误累计计数并刷新窗口，防 6 位数字验证码在线枚举
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, RESET_CODE_FAIL_TTL_SECONDS, TimeUnit.SECONDS);
            throw new BizException(AuthCodeEnum.VERIFY_CODE_INVALID);
        }
        redisTemplate.delete(failKey);
        applyNewPassword(po, dto.getNewPassword());
        redisTemplate.delete(RESET_CODE_KEY_PREFIX + dto.getAccount());
        invalidateRefreshTokens(po.getId());
        log.info("password reset, userId={}", po.getId());
    }

    private void applyNewPassword(AuthUserPO po, String newPassword) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt(BCRYPT_COST));
        po.setPasswordHash(hash);
        po.setSalt(salt);
        authUserManager.updateById(po);
    }

    private void updateLastLoginAt(AuthUserPO po) {
        po.setLastLoginAt(LocalDateTime.now());
        authUserManager.updateById(po);
    }

    private void invalidateRefreshTokens(Long userId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void revokeToken(String jti, Long userId, String reason) {
        // Persist to DB as source of truth
        AuthTokenBlacklistPO bl = new AuthTokenBlacklistPO();
        bl.setJti(jti);
        bl.setUserId(userId);
        bl.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        bl.setRevokedAt(LocalDateTime.now());
        bl.setReason(reason);
        tokenBlacklistManager.save(bl);
        // Also write to Redis for Gateway fast lookup
        redisTemplate.opsForValue().set(
            BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    private String resolveRoleCode(Long roleId) {
        if (roleId == null) {
            return "USER";
        }
        AuthRolePO role = authRoleManager.getById(roleId);
        return role != null ? role.getRoleCode() : "USER";
    }

    private TokenVO issueTokens(Long userId, String account, String roleCode, Long roleId) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtUtil.sign(userId, account, roleCode, roleId, jti);
        String refreshToken = jwtUtil.generateRefreshToken();
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + userId + ":" + jti,
            refreshToken, refreshTtlSeconds, TimeUnit.SECONDS);
        return new TokenVO(accessToken, refreshToken, "Bearer", ttlSeconds);
    }
}
