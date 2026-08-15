package com.yirancrazy.minimall.auth.service.impl;

import java.time.LocalDateTime;
import java.util.List;
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
import com.yirancrazy.minimall.auth.dto.AdminCreateDTO;
import com.yirancrazy.minimall.auth.dto.AdminPageDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPO;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.AdminVO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端认证服务实现，复用 t_auth_user 表与一致的密码、令牌、Redis 约定。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@Slf4j
@Service
public class PlatformAuthServiceImpl implements PlatformAuthService {

    private static final int BCRYPT_COST = 12;
    private static final int ACCOUNT_TYPE_PLATFORM = 3;
    private static final long DEFAULT_ROLE_ID_PLATFORM = 3L;

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";

    private final AuthUserManager authUserManager;
    private final AuthRoleManager authRoleManager;
    private final AuthTokenBlacklistManager tokenBlacklistManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final long refreshTtlSeconds;

    public PlatformAuthServiceImpl(AuthUserManager authUserManager,
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
     * 平台管理员登录，仅允许 accountType=PLATFORM 且 status=1 的账号通过。
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
        if (po.getAccountType() == null || po.getAccountType() != ACCOUNT_TYPE_PLATFORM) {
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
        log.info("platform admin login, accountId={}, account={}", po.getId(), po.getAccount());
        return issueTokens(po.getId(), po.getAccount(), po.getRoleId());
    }

    /**
     * 平台管理员登出，失效当前账号所有刷新令牌并拉黑当前 jti。
     * @param adminAccountId 管理员账号ID
     * @param jti 令牌唯一标识
     */
    @Override
    public void signOut(Long adminAccountId, String jti) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + adminAccountId + ":*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        revokeToken(jti, adminAccountId, "sign_out");
        log.info("platform admin signed out, accountId={}", adminAccountId);
    }

    /**
     * 创建平台管理员，账号不可重复，密码 BCrypt 加密存储。
     * @param dto 创建入参
     * @return 新管理员ID
     * @throws BizException 账号重复时
     */
    @Override
    public Long adminCreate(AdminCreateDTO dto) {
        AuthUserPO existing = authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPO.class).eq(AuthUserPO::getAccount, dto.getAccount()));
        if (existing != null) {
            throw new BizException(AuthCodeEnum.ADMIN_USERNAME_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AuthUserPO po = new AuthUserPO();
        po.setAccount(dto.getAccount());
        po.setPasswordHash(BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt(BCRYPT_COST)));
        po.setSalt(salt);
        po.setAccountType(ACCOUNT_TYPE_PLATFORM);
        po.setRoleId(DEFAULT_ROLE_ID_PLATFORM);
        po.setStatus(1);
        po.setNickname(dto.getNickname());
        authUserManager.save(po);
        log.info("admin created, id={}, account={}", po.getId(), dto.getAccount());
        return po.getId();
    }

    /**
     * 游标分页查询平台管理员，固定 accountType=PLATFORM，按 ID 降序返回。
     * @param dto 游标分页入参
     * @return 管理员游标分页结果
     */
    @Override
    public CursorPageVO<AdminVO> adminPage(AdminPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<AuthUserPO> records = authUserManager.list(Wrappers.lambdaQuery(AuthUserPO.class)
            .lt(lastId != null, AuthUserPO::getId, lastId)
            .eq(AuthUserPO::getAccountType, ACCOUNT_TYPE_PLATFORM)
            .orderByDesc(AuthUserPO::getId)
            .last("LIMIT " + (limit + 1)));
        CursorPageVO<AuthUserPO> poPage = CursorPageVO.of(records, limit, AuthUserPO::getId);
        return poPage.map(po -> new AdminVO(
            po.getId(),
            po.getAccount(),
            po.getNickname(),
            po.getStatus(),
            po.getCreateTime()));
    }

    /**
     * 更新平台管理员昵称。
     * @param id 管理员ID
     * @param dto 更新入参
     * @return 更新是否成功
     * @throws BizException 管理员不存在时
     */
    @Override
    public boolean adminUpdate(Long id, AdminUpdateDTO dto) {
        AuthUserPO po = authUserManager.getById(id);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        po.setNickname(dto.getNickname());
        boolean ok = authUserManager.updateById(po);
        log.info("admin updated, id={}", id);
        return ok;
    }

    /**
     * 逻辑删除平台管理员，不允许删除自己。
     * @param operatorId 操作人ID
     * @param targetId 目标管理员ID
     * @throws BizException 删除自己或管理员不存在时
     */
    @Override
    public void adminDelete(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BizException(AuthCodeEnum.CANNOT_DELETE_SELF);
        }
        AuthUserPO po = authUserManager.getById(targetId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        authUserManager.removeById(targetId);
        log.info("admin deleted, targetId={}, operatorId={}", targetId, operatorId);
    }

    /**
     * 获取当前平台管理员信息，从令牌声明解析，不落库。
     * @param token 访问令牌
     * @return 用户信息VO
     * @throws BizException 令牌无效时
     */
    @Override
    public UserInfoVO me(String token) {
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        }
        catch (JwtException ex) {
            throw new BizException(AuthCodeEnum.TOKEN_INVALID.getCode(),
                AuthCodeEnum.TOKEN_INVALID.getAlias(),
                AuthCodeEnum.TOKEN_INVALID.getMessage());
        }
        return new UserInfoVO(Long.parseLong(claims.getSubject()),
            claims.get("account", String.class),
            claims.get("role", String.class),
            claims.get("roleId", Long.class));
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
            return "PLATFORM";
        }
        AuthRolePO role = authRoleManager.getById(roleId);
        return role != null ? role.getRoleCode() : "PLATFORM";
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
