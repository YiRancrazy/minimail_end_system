package com.yirancrazy.minimall.auth.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPO;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.service.MerchantAuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端认证服务实现，复用 t_auth_user 表与一致的密码、令牌、Redis 约定。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Slf4j
@Service
public class MerchantAuthServiceImpl implements MerchantAuthService {

    private static final int BCRYPT_COST = 12;
    private static final int ACCOUNT_TYPE_MERCHANT = 2;
    private static final long DEFAULT_ROLE_ID_MERCHANT = 2L;

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";

    private final AuthUserManager authUserManager;
    private final AuthRoleManager authRoleManager;
    private final AuthTokenBlacklistManager tokenBlacklistManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final long refreshTtlSeconds;

    public MerchantAuthServiceImpl(AuthUserManager authUserManager,
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
     * 商家注册，创建 accountType=MERCHANT 的账号并签发令牌。
     * @param dto 注册DTO
     * @return 令牌VO
     */
    @Override
    public TokenVO register(RegisterDTO dto) {
        long existing = authUserManager.count(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (existing > 0) {
            throw new BizException(AuthCodeEnum.MERCHANT_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt(BCRYPT_COST));
        AuthUserPO po = new AuthUserPO();
        po.setAccount(dto.getAccount());
        po.setPasswordHash(hash);
        po.setSalt(salt);
        po.setAccountType(ACCOUNT_TYPE_MERCHANT);
        po.setRoleId(DEFAULT_ROLE_ID_MERCHANT);
        po.setStatus(1);
        authUserManager.save(po);
        log.info("merchant registered, accountId={}, account={}", po.getId(), po.getAccount());
        return issueTokens(po.getId(), po.getAccount(), po.getRoleId());
    }

    /**
     * 商家登录，仅允许 accountType=MERCHANT 且 status=1 的账号通过。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @Override
    public TokenVO login(LoginDTO dto) {
        AuthUserPO po = authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.MERCHANT_NOT_FOUND);
        }
        if (po.getAccountType() == null || po.getAccountType() != ACCOUNT_TYPE_MERCHANT) {
            throw new BizException(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH);
        }
        if (po.getStatus() == null || po.getStatus() != 1) {
            throw new BizException(AuthCodeEnum.ACCOUNT_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        po.setLastLoginAt(LocalDateTime.now());
        authUserManager.updateById(po);
        log.info("merchant login, accountId={}, account={}", po.getId(), po.getAccount());
        return issueTokens(po.getId(), po.getAccount(), po.getRoleId());
    }

    /**
     * 商家登出，失效当前账号所有刷新令牌并拉黑当前 jti。
     * @param merchantAccountId 商家账号ID
     * @param jti 令牌唯一标识
     */
    @Override
    public void signOut(Long merchantAccountId, String jti) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + merchantAccountId + ":*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        revokeToken(jti, merchantAccountId, "sign_out");
        log.info("merchant signed out, accountId={}", merchantAccountId);
    }

    /**
     * 商家修改密码，校验旧密码后设置新密码并失效已有刷新令牌。
     * @param merchantAccountId 商家账号ID
     * @param dto 修改密码DTO
     */
    @Override
    public void changePassword(Long merchantAccountId, ChangePasswordDTO dto) {
        AuthUserPO po = authUserManager.getById(merchantAccountId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.MERCHANT_NOT_FOUND);
        }
        if (!BCrypt.checkpw(dto.getOldPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        applyNewPassword(po, dto.getNewPassword());
        invalidateRefreshTokens(merchantAccountId);
        log.info("merchant password changed, accountId={}", merchantAccountId);
    }

    private void applyNewPassword(AuthUserPO po, String newPassword) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt(BCRYPT_COST));
        po.setPasswordHash(hash);
        po.setSalt(salt);
        authUserManager.updateById(po);
    }

    private void invalidateRefreshTokens(Long merchantAccountId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + merchantAccountId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private void revokeToken(String jti, Long userId, String reason) {
        AuthTokenBlacklistPO bl = new AuthTokenBlacklistPO();
        bl.setJti(jti);
        bl.setUserId(userId);
        bl.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        bl.setRevokedAt(LocalDateTime.now());
        bl.setReason(reason);
        tokenBlacklistManager.save(bl);
        redisTemplate.opsForValue().set(
            BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    private String resolveRoleCode(Long roleId) {
        if (roleId == null) {
            return "MERCHANT";
        }
        AuthRolePO role = authRoleManager.getById(roleId);
        return role != null ? role.getRoleCode() : "MERCHANT";
    }

    private TokenVO issueTokens(Long accountId, String account, Long roleId) {
        String roleCode = resolveRoleCode(roleId);
        String jti = UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtUtil.sign(accountId, account, roleCode, roleId, jti);
        String refreshToken = jwtUtil.generateRefreshToken();
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + accountId + ":" + jti,
            refreshToken, refreshTtlSeconds, TimeUnit.SECONDS);
        return new TokenVO(accessToken, refreshToken, "Bearer", ttlSeconds);
    }
}
