package com.yirancrazy.minimall.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.AdminCreateDto;
import com.yirancrazy.minimall.auth.dto.AdminPageDto;
import com.yirancrazy.minimall.auth.dto.AdminRoleDto;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.service.PlatformAuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.AdminVo;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员认证与管理核心实现；覆盖登录、会话撤销、管理员游标分页、角色分配和密码修改。
 * 管理员账号保存在 MySQL，刷新令牌、黑名单缓存和角色信息共同参与 JWT 签发与撤销。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
     * 注入账号、角色、撤销记录、JWT 和 Redis 依赖，并将毫秒配置转换为秒。
     *
     * @param authUserManager 认证账号数据访问入口
     * @param authRoleManager 角色数据访问入口
     * @param tokenBlacklistManager JWT 撤销记录数据访问入口
     * @param jwtUtil JWT 签发和解析工具
     * @param redisTemplate 刷新令牌和黑名单缓存
     * @param ttlSeconds 访问令牌有效时长，单位为毫秒
     * @param refreshTtlSeconds 刷新令牌有效时长，单位为毫秒
     */
    public PlatformAuthServiceImpl(
            AuthUserManager authUserManager,
            AuthRoleManager authRoleManager,
            AuthTokenBlacklistManager tokenBlacklistManager,
            JwtUtil jwtUtil,
            StringRedisTemplate redisTemplate,
            @Value("${minimall.jwt.ttl-ms:900000}") long ttlSeconds,
            @Value("${minimall.jwt.refresh-ttl-ms:604800000}") long refreshTtlSeconds) {
        this.authUserManager = authUserManager;
        this.authRoleManager = authRoleManager;
        this.tokenBlacklistManager = tokenBlacklistManager;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds / 1000;
        this.refreshTtlSeconds = refreshTtlSeconds / 1000;
    }

    /**
     * 校验平台管理员账号类型、启用状态和 BCrypt 密码，成功后更新最近登录时间并签发令牌。
     * 当前流程不包含普通用户登录使用的 Redis 失败次数锁定。
     *
     * @param dto 平台管理员登录账号和明文密码
     * @return 新签发的访问令牌和刷新令牌
     * @throws BizException 账号不存在、类型不匹配、禁用或密码错误时抛出
     */
    @Override
    public TokenVo login(LoginDto dto) {
        AuthUserPo po =
                authUserManager.getOne(
                        Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
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
     * 退出平台管理员会话：删除该账号全部刷新令牌，并将当前 jti 写入数据库黑名单和 Redis。
     *
     * @param adminAccountId 平台管理员认证账号 ID
     * @param jti 当前 JWT 唯一标识
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
     * 创建平台管理员：校验账号唯一后生成 8 位盐和 BCrypt 密码散列，保存平台类型账号。
     * 角色编码为空时使用默认平台角色，非空时解析对应角色；返回数据库生成的账号 ID。
     *
     * @param dto 管理员账号、密码、可选联系方式和角色编码
     * @return 新建管理员认证账号 ID
     * @throws BizException 账号重复或角色编码不存在时抛出
     */
    @Override
    public Long adminCreate(AdminCreateDto dto) {
        AuthUserPo existing =
                authUserManager.getOne(
                        Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
        if (existing != null) {
            throw new BizException(AuthCodeEnum.ADMIN_USERNAME_EXISTS);
        }
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AuthUserPo po = new AuthUserPo();
        po.setAccount(dto.getAccount());
        po.setPasswordHash(BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt(BCRYPT_COST)));
        po.setSalt(salt);
        po.setAccountType(ACCOUNT_TYPE_PLATFORM);
        po.setRoleId(resolveRoleId(dto.getRoleCode()));
        po.setStatus(1);
        po.setContact(dto.getContact());
        authUserManager.save(po);
        log.info("管理员已创建，id={}，account={}，roleId={}", po.getId(), po.getAccount(), po.getRoleId());
        return po.getId();
    }

    /**
     * 使用 ID 游标倒序查询平台类型账号，额外读取一条记录判断是否还有下一页。
     * 返回前将持久化账号转换为不包含密码散列和盐值的 AdminVo。
     *
     * @param dto 游标和每页数量
     * @return 平台管理员游标分页结果
     */
    @Override
    public CursorPageVo<AdminVo> adminPage(AdminPageDto dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<AuthUserPo> records =
                authUserManager.list(
                        Wrappers.lambdaQuery(AuthUserPo.class)
                                .lt(lastId != null, AuthUserPo::getId, lastId)
                                .eq(AuthUserPo::getAccountType, ACCOUNT_TYPE_PLATFORM)
                                .orderByDesc(AuthUserPo::getId)
                                .last("LIMIT " + (limit + 1)));
        CursorPageVo<AuthUserPo> poPage = CursorPageVo.of(records, limit, AuthUserPo::getId);
        return poPage.map(
                po ->
                        new AdminVo(
                                po.getId(),
                                po.getAccount(),
                                po.getStatus(),
                                po.getCreateTime(),
                                resolveRoleCode(po.getRoleId()),
                                po.getContact()));
    }

    /**
     * 将目标认证账号绑定到角色编码对应的角色 ID 并更新数据库。
     *
     * @param id 目标认证账号 ID
     * @param dto 新角色编码
     * @throws BizException 目标账号或角色不存在时抛出
     */
    public void adminAssignRole(Long id, AdminRoleDto dto) {
        AuthUserPo po = authUserManager.getById(id);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        po.setRoleId(resolveRoleId(dto.getRoleCode()));
        authUserManager.updateById(po);
        log.info("管理员角色已分配，id={}，roleId={}", id, po.getRoleId());
    }

    /**
     * 校验旧密码后更新平台管理员密码，并删除该账号的全部刷新令牌。
     *
     * @param adminAccountId 平台管理员认证账号 ID
     * @param dto 旧密码和新密码
     * @throws BizException 账号不存在或旧密码错误时抛出
     */
    public void changePassword(Long adminAccountId, ChangePasswordDto dto) {
        AuthUserPo po = authUserManager.getById(adminAccountId);
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
     * 使用新的随机盐和 BCrypt 成本 12 生成密码散列，并持久化管理员账号。
     *
     * @param po 待更新的管理员账号
     * @param newPassword 新明文密码
     */
    private void applyNewPassword(AuthUserPo po, String newPassword) {
        String salt = UUID.randomUUID().toString().replace("-", "");
        String hash = BCrypt.hashpw(newPassword + salt, BCrypt.gensalt(BCRYPT_COST));
        po.setPasswordHash(hash);
        po.setSalt(salt);
        authUserManager.updateById(po);
    }

    /**
     * 删除指定管理员账号在 Redis 中以 refresh:{adminAccountId}: 开头的全部刷新令牌。
     *
     * @param adminAccountId 平台管理员认证账号 ID
     */
    private void invalidateRefreshTokens(Long adminAccountId) {
        var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + adminAccountId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 删除指定管理员账号；操作者与目标相同时拒绝，目标不存在时返回业务错误。
     * 删除前不额外校验目标账号类型，调用方应通过平台管理员接口传入目标 ID。
     *
     * @param operatorId 当前操作者账号 ID
     * @param targetId 待删除账号 ID
     * @throws BizException 删除自身或目标不存在时抛出
     */
    public void adminDelete(Long operatorId, Long targetId) {
        if (operatorId.equals(targetId)) {
            throw new BizException(AuthCodeEnum.CANNOT_DELETE_SELF);
        }
        AuthUserPo po = authUserManager.getById(targetId);
        if (po == null) {
            throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
        }
        authUserManager.removeById(targetId);
        log.info("管理员已删除，targetId={}，operatorId={}", targetId, operatorId);
    }

    /**
     * 校验 JWT 并从声明中恢复平台管理员身份；管理员没有独立业务用户 ID，userId 等于认证账号 ID。
     *
     * @param token 不带 Bearer 前缀的 JWT
     * @return 管理员认证账号、账号和角色信息
     * @throws BizException JWT 格式错误、签名不匹配或已过期时抛出
     */
    @Override
    public UserInfoVo me(String token) {
        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (JwtException ex) {
            throw new BizException(
                    AuthCodeEnum.TOKEN_INVALID.getCode(),
                    AuthCodeEnum.TOKEN_INVALID.getAlias(),
                    AuthCodeEnum.TOKEN_INVALID.getMessage());
        }
        Long authUserId = Long.parseLong(claims.getSubject());
        return new UserInfoVo(
                authUserId,
                authUserId,
                claims.get("account", String.class),
                claims.get("role", String.class),
                claims.get("roleId", Long.class));
    }

    /**
     * 将指定 jti 写入数据库黑名单和 Redis；缓存有效时长与访问令牌一致。
     *
     * @param jti JWT 唯一标识
     * @param userId 管理员认证账号 ID
     * @param reason 撤销原因，写入持久化记录
     */
    private void revokeToken(String jti, Long userId, String reason) {
        AuthTokenBlacklistPo bl = new AuthTokenBlacklistPo();
        bl.setJti(jti);
        bl.setUserId(userId);
        bl.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
        bl.setRevokedAt(LocalDateTime.now());
        bl.setReason(reason);
        tokenBlacklistManager.save(bl);
        redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 根据角色 ID 查询角色编码；角色 ID 为空或角色不存在时回退为 PLATFORM。
     *
     * @param roleId 持久化角色 ID，可为空
     * @return 可写入 JWT 的角色编码
     */
    private String resolveRoleCode(Long roleId) {
        if (roleId == null) {
            return "PLATFORM";
        }
        AuthRolePo role = authRoleManager.getById(roleId);
        return role != null ? role.getRoleCode() : "PLATFORM";
    }

    /**
     * 根据角色编码查询角色 ID；编码为空时返回平台默认角色 ID。
     *
     * @param roleCode 角色编码
     * @return 持久化角色 ID
     * @throws BizException 角色编码存在但查不到对应角色时抛出
     */
    private Long resolveRoleId(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return DEFAULT_ROLE_ID_PLATFORM;
        }
        AuthRolePo role =
                authRoleManager.getOne(
                        Wrappers.lambdaQuery(AuthRolePo.class).eq(AuthRolePo::getRoleCode, roleCode));
        if (role == null) {
            throw new BizException(AuthCodeEnum.ROLE_NOT_FOUND);
        }
        return role.getId();
    }

    /**
     * 签发平台管理员访问 JWT 和随机刷新令牌，并将刷新令牌保存到 Redis。
     * 访问 JWT 的认证账号 ID 与业务用户 ID 均使用管理员认证账号 ID。
     *
     * @param accountId 管理员认证账号 ID
     * @param account 账号
     * @param roleId 角色 ID
     * @return 访问令牌、刷新令牌、Bearer 类型和访问令牌有效秒数
     */
    private TokenVo issueTokens(Long accountId, String account, Long roleId) {
        String roleCode = resolveRoleCode(roleId);
        String jti = UUID.randomUUID().toString().replace("-", "");
        String accessToken =
                jwtUtil.sign(
                        accountId,
                        accountId,
                        account,
                        roleCode,
                        roleId,
                        jti,
                        JwtType.ACCESS,
                        ttlSeconds * 1000);
        String refreshToken = jwtUtil.generateRefreshToken();
        redisTemplate
                .opsForValue()
                .set(
                        REFRESH_KEY_PREFIX + accountId + ":" + jti,
                        refreshToken,
                        refreshTtlSeconds,
                        TimeUnit.SECONDS);
        return new TokenVo(accessToken, refreshToken, "Bearer", ttlSeconds);
    }
}
