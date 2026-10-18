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
import com.yirancrazy.minimall.auth.dto.AdminRoleDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
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
 * @Description: PlatformAuthServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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

    /**
     * 构造 PlatformAuthServiceImpl 实例。
     * @param authUserManager authUserManager 参数
     * @param authRoleManager authRoleManager 参数
     * @param tokenBlacklistManager tokenBlacklistManager 参数
     * @param jwtUtil jwtUtil 参数
     * @param redisTemplate redisTemplate 参数
     * @param ttlSeconds ttlSeconds 参数
     * @param refreshTtlSeconds refreshTtlSeconds 参数
     */
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
     * 执行 login 方法。
     * @param dto dto 参数
     * @return 处理结果
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
        log.info("平台管理员已登录，accountId={}，account={}", po.getId(), po.getAccount());
        return issueTokens(po.getId(), po.getAccount(), po.getRoleId());
    }


    /**
     * 执行 signOut 方法。
     * @param adminAccountId adminAccountId 参数
     * @param jti jti 参数
     */
    public void signOut(Long adminAccountId, String jti) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + adminAccountId + ":*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        revokeToken(jti, adminAccountId, "sign_out");
        log.info("平台管理员已退出登录，accountId={}", adminAccountId);
    }


    /**
     * 执行 adminCreate 方法。
     * @param dto dto 参数
     * @return 业务数值
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
        po.setRoleId(resolveRoleId(dto.getRoleCode()));
        po.setStatus(1);
        po.setNickname(dto.getNickname());
        po.setContact(dto.getContact());
        authUserManager.save(po);
        log.info("管理员已创建，id={}，account={}，roleId={}", po.getId(), po.getAccount(), po.getRoleId());
        return po.getId();
    }


    /**
     * 执行 adminPage 方法。
     * @param dto dto 参数
     * @return 处理结果
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
            po.getCreateTime(),
            resolveRoleCode(po.getRoleId()),
            po.getContact()));
    }


    /**
     * 执行 adminAssignRole 方法。
     * @param id id 参数
     * @param dto dto 参数
     */
    public void adminAssignRole(Long id, AdminRoleDTO dto) {
        AuthUserPO po = authUserManager.getById(id);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        po.setRoleId(resolveRoleId(dto.getRoleCode()));
        authUserManager.updateById(po);
        log.info("管理员角色已分配，id={}，roleId={}", id, po.getRoleId());
    }


    /**
     * 执行 changePassword 方法。
     * @param adminAccountId adminAccountId 参数
     * @param dto dto 参数
     */
    public void changePassword(Long adminAccountId, ChangePasswordDTO dto) {
        AuthUserPO po = authUserManager.getById(adminAccountId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(dto.getOldPassword() + po.getSalt(), po.getPasswordHash())) {
            throw new BizException(AuthCodeEnum.PWD_INVALID);
        }
        applyNewPassword(po, dto.getNewPassword());
        invalidateRefreshTokens(adminAccountId);
        log.info("平台管理员密码已修改，accountId={}", adminAccountId);
    }

    /**
     * 执行新密码逻辑。
     * @param po 持久化对象
     * @param newPassword 新密码
     */
    private void applyNewPassword(AuthUserPO po, String newPassword) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt(BCRYPT_COST));
        po.setPasswordHash(hash);
        po.setSalt(salt);
        authUserManager.updateById(po);
    }

    /**
     * 使刷新Token失效。
     * @param adminAccountId admin账号 ID
     */
    private void invalidateRefreshTokens(Long adminAccountId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + adminAccountId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }


    /**
     * 执行 adminUpdate 方法。
     * @param id id 参数
     * @param dto dto 参数
     * @return 是否处理成功
     */
    @Override
    public boolean adminUpdate(Long id, AdminUpdateDTO dto) {
        AuthUserPO po = authUserManager.getById(id);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        po.setNickname(dto.getNickname());
        boolean ok = authUserManager.updateById(po);
        log.info("管理员已更新，id={}", id);
        return ok;
    }


    /**
     * 执行 adminDelete 方法。
     * @param operatorId operatorId 参数
     * @param targetId targetId 参数
     */
    public void adminDelete(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BizException(AuthCodeEnum.CANNOT_DELETE_SELF);
        }
        AuthUserPO po = authUserManager.getById(targetId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        authUserManager.removeById(targetId);
        log.info("管理员已删除，targetId={}，operatorId={}", targetId, operatorId);
    }


    /**
     * 执行 me 方法。
     * @param token token 参数
     * @return 处理结果
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

    /**
     * 吊销Token。
     * @param jti JWT ID
     * @param userId 用户 ID
     * @param reason 原因
     */
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

    /**
     * 转换或解析角色编码。
     * @param roleId 角色 ID
     * @return 编号或编码
     */
    private String resolveRoleCode(Long roleId) {
        if (roleId == null) {
            return "PLATFORM";
        }
        AuthRolePO role = authRoleManager.getById(roleId);
        return role != null ? role.getRoleCode() : "PLATFORM";
    }

    /**
     * 转换或解析角色ID。
     * @param roleCode 角色编码
     * @return 处理结果
     * @throws BizException 业务校验失败时抛出
     */
    private Long resolveRoleId(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return DEFAULT_ROLE_ID_PLATFORM;
        }
        AuthRolePO role = authRoleManager.getOne(
            Wrappers.lambdaQuery(AuthRolePO.class).eq(AuthRolePO::getRoleCode, roleCode));
        if (role == null) {
            throw new BizException(AuthCodeEnum.ROLE_NOT_FOUND);
        }
        return role.getId();
    }

    /**
     * 签发Token。
     * @param accountId 账号 ID
     * @param account 账号
     * @param roleId 角色 ID
     * @return Token 信息
     */
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
