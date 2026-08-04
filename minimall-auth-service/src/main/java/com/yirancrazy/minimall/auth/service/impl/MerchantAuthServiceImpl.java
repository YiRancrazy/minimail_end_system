package com.yirancrazy.minimall.auth.service.impl;

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
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.service.MerchantAuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端认证服务实现，复用 t_user_auth 表与 USER 端一致的密码、令牌、Redis 约定。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Slf4j
@Service
public class MerchantAuthServiceImpl implements MerchantAuthService {

    private static final String ROLE_MERCHANT = "MERCHANT";
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";

    private final UserAuthManager userAuthManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final long refreshTtlSeconds;

    public MerchantAuthServiceImpl(UserAuthManager userAuthManager,
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
     * 商家注册，创建 role=MERCHANT 的账号并签发令牌。
     * @param dto 注册DTO
     * @return 令牌VO
     */
    @Override
    public TokenVO register(RegisterDTO dto) {
        long existing = userAuthManager.count(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (existing > 0) {
            throw new BizException(AuthCodeEnum.MERCHANT_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setUsername(dto.getUsername());
        po.setPasswordHash(hash);
        po.setSalt(salt);
        po.setRole(ROLE_MERCHANT);
        po.setStatus(1);
        userAuthManager.save(po);
        log.info("merchant registered, accountId={}, username={}", po.getId(), po.getUsername());
        return issueTokens(po.getId(), po.getUsername());
    }

    /**
     * 商家登录，仅允许 role=MERCHANT 且 status=1 的账号通过。
     * @param dto 登录DTO
     * @return 令牌VO
     */
    @Override
    public TokenVO login(LoginDTO dto) {
        UserAuthPO po = userAuthManager.getOne(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (po == null) {
            throw new BizException(AuthCodeEnum.MERCHANT_NOT_FOUND);
        }
        if (!ROLE_MERCHANT.equals(po.getRole())) {
            throw new BizException(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH);
        }
        if (po.getStatus() == null || po.getStatus() != 1) {
            throw new BizException(AuthCodeEnum.ACCOUNT_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        log.info("merchant login, accountId={}, username={}", po.getId(), po.getUsername());
        return issueTokens(po.getId(), po.getUsername());
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
        redisTemplate.opsForValue().set(
            BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        log.info("merchant signed out, accountId={}", merchantAccountId);
    }

    /**
     * 商家修改密码，校验旧密码后设置新密码并失效已有刷新令牌。
     * @param merchantAccountId 商家账号ID
     * @param dto 修改密码DTO
     */
    @Override
    public void changePassword(Long merchantAccountId, ChangePasswordDTO dto) {
        UserAuthPO po = userAuthManager.getById(merchantAccountId);
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

    private void applyNewPassword(UserAuthPO po, String newPassword) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt());
        po.setPasswordHash(hash);
        po.setSalt(salt);
        userAuthManager.updateById(po);
    }

    private void invalidateRefreshTokens(Long merchantAccountId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + merchantAccountId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private TokenVO issueTokens(Long accountId, String username) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtUtil.sign(accountId, username, ROLE_MERCHANT, jti);
        String refreshToken = jwtUtil.generateRefreshToken();
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + accountId + ":" + jti,
            refreshToken, refreshTtlSeconds, TimeUnit.SECONDS);
        return new TokenVO(accessToken, refreshToken, "Bearer", ttlSeconds);
    }
}
