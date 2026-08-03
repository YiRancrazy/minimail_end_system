package com.yirancrazy.minimall.auth.service.impl;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.AdminCreateDTO;
import com.yirancrazy.minimall.auth.dto.AdminPageDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端认证服务实现，复用 t_user_auth 表与一致的密码、令牌、Redis 约定。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Slf4j
@Service
public class PlatformAuthServiceImpl implements PlatformAuthService {

    private static final String ROLE_PLATFORM = "PLATFORM";
    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jti:";

    private final UserAuthManager userAuthManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final long ttlSeconds;
    private final long refreshTtlSeconds;

    public PlatformAuthServiceImpl(UserAuthManager userAuthManager,
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
     * 平台管理员登录，仅允许 role=PLATFORM 且 status=1 的账号通过。
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
        if (!ROLE_PLATFORM.equals(po.getRole())) {
            throw new BizException(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH);
        }
        if (po.getStatus() == null || po.getStatus() != 1) {
            throw new BizException(AuthCodeEnum.ACCOUNT_DISABLED);
        }
        if (!BCrypt.checkpw(dto.getPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        log.info("platform admin login, accountId={}, username={}", po.getId(), po.getUsername());
        return issueTokens(po.getId(), po.getUsername());
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
        redisTemplate.opsForValue().set(
            BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
        log.info("platform admin signed out, accountId={}", adminAccountId);
    }

    /**
     * 创建平台管理员，用户名不可重复，密码 BCrypt 加密存储。
     * @param dto 创建入参
     * @return 新管理员ID
     * @throws BizException 用户名重复时
     */
    @Override
    public Long adminCreate(AdminCreateDTO dto) {
        UserAuthPO existing = userAuthManager.getOne(
            Wrappers.lambdaQuery(UserAuthPO.class).eq(UserAuthPO::getUsername, dto.getUsername()));
        if (existing != null) {
            throw new BizException(AuthCodeEnum.ADMIN_USERNAME_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        UserAuthPO po = new UserAuthPO();
        po.setUsername(dto.getUsername());
        po.setPasswordHash(BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt()));
        po.setSalt(salt);
        po.setRole(ROLE_PLATFORM);
        po.setStatus(1);
        po.setNickname(dto.getNickname());
        userAuthManager.save(po);
        log.info("admin created, id={}, username={}", po.getId(), dto.getUsername());
        return po.getId();
    }

    /**
     * 分页查询平台管理员，固定 role=PLATFORM，按 ID 降序返回。
     * @param dto 分页入参
     * @return 管理员分页结果
     */
    @Override
    public IPage<UserAuthPO> adminPage(AdminPageDTO dto) {
        Page<UserAuthPO> page = new Page<>(dto.getPageNo(), dto.getPageSize());
        return userAuthManager.page(page, Wrappers.lambdaQuery(UserAuthPO.class)
            .eq(UserAuthPO::getRole, ROLE_PLATFORM)
            .orderByDesc(UserAuthPO::getId));
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
        UserAuthPO po = userAuthManager.getById(id);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        po.setNickname(dto.getNickname());
        boolean ok = userAuthManager.updateById(po);
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
        UserAuthPO po = userAuthManager.getById(targetId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        userAuthManager.removeById(targetId);
        log.info("admin deleted, targetId={}, operatorId={}", targetId, operatorId);
    }

    private TokenVO issueTokens(Long accountId, String username) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        String accessToken = jwtUtil.sign(accountId, username, ROLE_PLATFORM, jti);
        String refreshToken = jwtUtil.generateRefreshToken();
        redisTemplate.opsForValue().set(
            REFRESH_KEY_PREFIX + accountId + ":" + jti,
            refreshToken, refreshTtlSeconds, TimeUnit.SECONDS);
        return new TokenVO(accessToken, refreshToken, "Bearer", ttlSeconds);
    }
}
