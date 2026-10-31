package com.yirancrazy.minimall.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDto;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDto;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.gateway.AuthRemoteGateway;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.service.AuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.seata.spring.annotation.GlobalTransactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 普通用户认证核心实现；负责账号档案创建、登录失败锁定、JWT 签发与刷新轮换。
 * 账号数据写入 MySQL，刷新令牌、失败计数和黑名单索引写入 Redis；远程用户档案由网关创建。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
  private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:";
  // 登录失败锁定窗口：10 分钟内累计 5 次失败后临时锁定
  private static final long LOGIN_FAIL_TTL_SECONDS = 600L;
  private static final long LOGIN_FAIL_THRESHOLD = 5L;
  // 刷新令牌轮换必须原子：比较并删除成功者才能签发新令牌，否则并发重放会各自换出令牌
  private static final RedisScript<Long> COMPARE_AND_DELETE_REFRESH =
      RedisScript.of(
          "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else"
              + " return 0 end",
          Long.class);

  private final AuthUserManager authUserManager;
  private final AuthRoleManager authRoleManager;
  private final AuthTokenBlacklistManager tokenBlacklistManager;
  private final AuthRemoteGateway authRemoteGateway;
  private final JwtUtil jwtUtil;
  private final StringRedisTemplate redisTemplate;
  private final long ttlSeconds;
  private final long refreshTtlSeconds;

  /**
   * 注入账号、角色、撤销记录、远程档案、JWT 和 Redis 依赖，并将毫秒配置转换为秒。
   *
   * @param authUserManager 认证账号数据访问入口
   * @param authRoleManager 角色数据访问入口
   * @param tokenBlacklistManager JWT 撤销记录数据访问入口
   * @param authRemoteGateway 业务用户 ID 与档案远程入口
   * @param jwtUtil JWT 签发和解析工具
   * @param redisTemplate 刷新令牌、失败计数和黑名单缓存
   * @param ttlSeconds 访问令牌有效时长，单位为毫秒
   * @param refreshTtlSeconds 刷新令牌有效时长，单位为毫秒
   */
  public AuthServiceImpl(
      AuthUserManager authUserManager,
      AuthRoleManager authRoleManager,
      AuthTokenBlacklistManager tokenBlacklistManager,
      AuthRemoteGateway authRemoteGateway,
      JwtUtil jwtUtil,
      StringRedisTemplate redisTemplate,
      @Value("${minimall.jwt.ttl-ms:900000}") long ttlSeconds,
      @Value("${minimall.jwt.refresh-ttl-ms:604800000}") long refreshTtlSeconds) {
    this.authUserManager = authUserManager;
    this.authRoleManager = authRoleManager;
    this.tokenBlacklistManager = tokenBlacklistManager;
    this.authRemoteGateway = authRemoteGateway;
    this.jwtUtil = jwtUtil;
    this.redisTemplate = redisTemplate;
    this.ttlSeconds = ttlSeconds / 1000;
    this.refreshTtlSeconds = refreshTtlSeconds / 1000;
  }

  /**
   * 注册普通用户：先确认账号唯一并创建业务档案，再保存认证账号并签发令牌。
   * 密码使用随机盐和 BCrypt 散列；用户类型固定为 1，默认角色为普通用户。
   *
   * @param dto 注册账号和明文密码
   * @return 访问令牌、刷新令牌和令牌类型
   * @throws BizException 账号已存在、用户 ID 生成失败或档案创建失败时抛出
   */
  @Override
  @GlobalTransactional
  public TokenVo register(RegisterDto dto) {
    AuthUserPo existing =
        authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
    if (existing != null) {
      throw new BizException(AuthCodeEnum.USER_EXISTS);
    }
    Long userId = authRemoteGateway.nextUserId();
    authRemoteGateway.provisionUserProfile(userId);

    String salt = UUID.randomUUID().toString().replace("-", "");
    String hash = BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt(BCRYPT_COST));
    AuthUserPo po = new AuthUserPo();
    po.setUserId(userId);
    po.setAccount(dto.getAccount());
    po.setPasswordHash(hash);
    po.setSalt(salt);
    po.setAccountType(ACCOUNT_TYPE_USER);
    po.setRoleId(DEFAULT_ROLE_ID_USER);
    po.setStatus(1);
    authUserManager.save(po);
    String roleCode = resolveRoleCode(po.getRoleId());
    return issueTokens(po.getId(), userId, po.getAccount(), roleCode, po.getRoleId());
  }

  /**
   * 校验普通用户登录；Redis 中同一账号失败达到 5 次时直接拒绝，成功登录清除失败计数。
   * 校验顺序为账号存在、失败锁定、账号类型、启用状态和 BCrypt 密码。
   *
   * @param dto 登录账号和明文密码
   * @return 新签发的访问令牌和刷新令牌
   * @throws BizException 账号不存在、锁定、类型不匹配、禁用或密码错误时抛出
   */
  @Override
  public TokenVo login(LoginDto dto) {
    AuthUserPo po =
        authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
    if (po == null) {
      throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
    }
    // 失败计数已达阈值时直接拒绝，防密码在线枚举
    String failKey = LOGIN_FAIL_KEY_PREFIX + dto.getAccount();
    String failCount = redisTemplate.opsForValue().get(failKey);
    if (failCount != null && Long.parseLong(failCount) >= LOGIN_FAIL_THRESHOLD) {
      throw new BizException(AuthCodeEnum.LOGIN_ATTEMPT_EXCEEDED);
    }
    if (po.getAccountType() == null || po.getAccountType() != ACCOUNT_TYPE_USER) {
      throw new BizException(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH);
    }
    if (po.getStatus() == null || po.getStatus() != 1) {
      throw new BizException(AuthCodeEnum.ACCOUNT_DISABLED);
    }
    if (!BCrypt.checkpw(dto.getPassword() + po.getSalt(), po.getPasswordHash())) {
      // 失败计数累加并刷新窗口，成功后清除
      redisTemplate.opsForValue().increment(failKey);
      redisTemplate.expire(failKey, LOGIN_FAIL_TTL_SECONDS, TimeUnit.SECONDS);
      throw new BizException(AuthCodeEnum.PWD_INVALID);
    }
    Long userId = requireUserProfileId(po);
    redisTemplate.delete(failKey);
    updateLastLoginAt(po);
    String roleCode = resolveRoleCode(po.getRoleId());
    return issueTokens(po.getId(), userId, po.getAccount(), roleCode, po.getRoleId());
  }

  /**
   * 解析刷新 JWT，并通过 Redis Lua 比较删除保证同一条刷新记录只能被一个请求消费。
   * 消费成功后撤销旧 jti、重新加载账号并签发新令牌；并发重放会返回无效刷新令牌。
   *
   * @param refreshToken 已签名的刷新 JWT
   * @return 轮换后的访问令牌和刷新令牌
   * @throws BizException JWT 无效、非刷新类型、记录已消费或用户不存在时抛出
   */
  @Override
  public TokenVo refreshToken(String refreshToken) {
    // 刷新令牌为JWT，解析后携带 userId(subject) 与 jti，可直查索引键避免 KEYS 全表扫描
    Claims c;
    try {
      c = jwtUtil.parse(refreshToken);
    } catch (JwtException ex) {
      throw new BizException(
          AuthCodeEnum.REFRESH_TOKEN_INVALID.getCode(),
          AuthCodeEnum.REFRESH_TOKEN_INVALID.getAlias(),
          AuthCodeEnum.REFRESH_TOKEN_INVALID.getMessage());
    }
    Long userId = Long.parseLong(c.getSubject());
    String jti = c.get("jti", String.class);
    String type = c.get("type", String.class);

    if (type == null || !type.equals(JwtType.REFRESH)) {
      throw new BizException(AuthCodeEnum.REFRESH_TOKEN_INVALID);
    }

    String key = REFRESH_KEY_PREFIX + userId + ":" + jti;
    Long deleted = redisTemplate.execute(COMPARE_AND_DELETE_REFRESH, List.of(key), refreshToken);
    if (deleted == null || deleted == 0L) {
      throw new BizException(AuthCodeEnum.REFRESH_TOKEN_INVALID);
    }
    revokeToken(jti, userId, "refresh_rotated");

    AuthUserPo po = authUserManager.getById(userId);
    if (po == null) {
      throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
    }
    Long userProfileId = requireUserProfileId(po);
    log.info("refresh token 已轮换，userId={}", userId);
    String roleCode = resolveRoleCode(po.getRoleId());
    return issueTokens(po.getId(), userProfileId, po.getAccount(), roleCode, po.getRoleId());
  }

  /**
   * 退出普通用户会话：删除该账号全部刷新令牌，并将当前 jti 写入数据库黑名单和 Redis。
   *
   * @param userId 认证账号 ID
   * @param jti 当前 JWT 唯一标识
   */
  public void signOut(Long userId, String jti) {
    var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + userId + ":*");
    if (keys != null) {
      redisTemplate.delete(keys);
    }
    revokeToken(jti, userId, "sign_out");
    log.info("用户已退出登录，userId={}", userId);
  }

  /**
   * 校验 JWT 并从声明中恢复当前用户信息。
   * 业务用户 ID 缺失时回退为认证账号 ID；本方法不查询数据库。
   *
   * @param token 不带 Bearer 前缀的 JWT
   * @return 认证账号与业务用户的身份信息
   * @throws BizException JWT 格式错误、签名不匹配或已过期时抛出
   */
  @Override
  public UserInfoVo me(String token) {
    Claims c;
    try {
      c = jwtUtil.parse(token);
    } catch (JwtException ex) {
      throw new BizException(
          AuthCodeEnum.TOKEN_INVALID.getCode(),
          AuthCodeEnum.TOKEN_INVALID.getAlias(),
          AuthCodeEnum.TOKEN_INVALID.getMessage());
    }
    Long authUserId = Long.parseLong(c.getSubject());
    String userProfileId = c.get("userId", String.class);
    Long businessUserId =
        userProfileId == null || userProfileId.isBlank()
            ? authUserId
            : Long.parseLong(userProfileId);
    return new UserInfoVo(
        authUserId,
        businessUserId,
        c.get("account", String.class),
        c.get("role", String.class),
        c.get("roleId", Long.class));
  }

  /**
   * 校验旧密码后生成新盐和 BCrypt 散列，并删除该账号的全部刷新令牌。
   * 已签发的访问令牌不会在本方法内逐条撤销，仍需等待过期或由撤销流程处理。
   *
   * @param userId 认证账号 ID
   * @param dto 旧密码和新密码
   * @throws BizException 账号不存在或旧密码错误时抛出
   */
  public void changePassword(Long userId, ChangePasswordDto dto) {
    AuthUserPo po = authUserManager.getById(userId);
    if (po == null) {
      throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
    }
    if (!BCrypt.checkpw(dto.getOldPassword() + po.getSalt(), po.getPasswordHash())) {
      throw new BizException(AuthCodeEnum.PWD_INVALID);
    }
    applyNewPassword(po, dto.getNewPassword());
    invalidateRefreshTokens(userId);
    log.info("密码已修改，userId={}", userId);
  }

  /**
   * 为已存在账号生成 6 位密码重置码，并用 Redis 原子冷却键限制 60 秒内重复申请。
   * 验证码保存 600 秒；当前实现只记录日志，尚未接入真实短信发送通道。
   *
   * @param dto 待重置密码的账号
   * @throws BizException 账号不存在或仍在发送冷却期时抛出
   */
  public void sendResetCode(SendResetCodeDto dto) {
    AuthUserPo po =
        authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
    if (po == null) {
      throw new BizException(AuthCodeEnum.USER_NOT_FOUND);
    }
    // setIfAbsent 原子占位冷却键，防止对同一账号轰炸验证码请求
    String cooldownKey = RESET_CODE_COOLDOWN_KEY_PREFIX + dto.getAccount();
    Boolean acquired =
        redisTemplate
            .opsForValue()
            .setIfAbsent(cooldownKey, "1", RESET_CODE_COOLDOWN_SECONDS, TimeUnit.SECONDS);
    if (!Boolean.TRUE.equals(acquired)) {
      throw new BizException(AuthCodeEnum.RESET_CODE_TOO_FREQUENT);
    }
    String code =
        String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1_000_000));
    redisTemplate
        .opsForValue()
        .set(
            RESET_CODE_KEY_PREFIX + dto.getAccount(),
            code,
            RESET_CODE_TTL_SECONDS,
            TimeUnit.SECONDS);
    // 首轮迭代短信通道尚未就绪；记录验证码便于联调排查。
    log.info("重置验证码已生成，account={}", dto.getAccount());
  }

  /**
   * 校验重置验证码并更新密码；错误验证码会累计失败次数，达到 5 次后拒绝后续尝试。
   * 成功后清除失败计数和验证码，删除该账号的刷新令牌。
   *
   * @param dto 账号、6 位验证码和新密码
   * @throws BizException 账号不存在、验证码错误或尝试次数超限时抛出
   */
  public void resetPassword(ResetPasswordDto dto) {
    AuthUserPo po =
        authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
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
    log.info("密码已重置，userId={}", po.getId());
  }

  /**
   * 使用新的随机盐和 BCrypt 成本 12 生成密码散列，并持久化账号。
   *
   * @param po 待更新的认证账号
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
   * 将账号的最近登录时间设置为当前时间并更新数据库。
   *
   * @param po 已完成密码校验的认证账号
   */
  private void updateLastLoginAt(AuthUserPo po) {
    po.setLastLoginAt(LocalDateTime.now());
    authUserManager.updateById(po);
  }

  /**
   * 删除指定认证账号在 Redis 中以 refresh:{userId}: 开头的全部刷新令牌。
   * 该方法使用 KEYS 模式扫描，调用频率受认证操作边界约束。
   *
   * @param userId 认证账号 ID
   */
  private void invalidateRefreshTokens(Long userId) {
    var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + userId + ":*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  /**
   * 将指定 jti 写入数据库黑名单和 Redis，有效时长与访问令牌一致。
   * 数据库作为可信撤销记录，Redis 作为网关快速查询缓存。
   *
   * @param jti JWT 唯一标识
   * @param userId 认证账号 ID
   * @param reason 撤销原因，写入持久化记录
   */
  private void revokeToken(String jti, Long userId, String reason) {
    // 持久化到 DB，作为可信数据源
    AuthTokenBlacklistPo bl = new AuthTokenBlacklistPo();
    bl.setJti(jti);
    bl.setUserId(userId);
    bl.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));
    bl.setRevokedAt(LocalDateTime.now());
    bl.setReason(reason);
    tokenBlacklistManager.save(bl);
    // 同时写入 Redis，供 Gateway 快速查询
    redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + jti, "1", ttlSeconds, TimeUnit.SECONDS);
  }

  /**
   * 根据角色 ID 查询角色编码；角色 ID 为空或角色不存在时回退为 USER。
   *
   * @param roleId 持久化角色 ID，可为空
   * @return 可写入 JWT 的角色编码
   */
  private String resolveRoleCode(Long roleId) {
    if (roleId == null) {
      return "USER";
    }
    AuthRolePo role = authRoleManager.getById(roleId);
    return role != null ? role.getRoleCode() : "USER";
  }

  /**
   * 为普通用户签发同一 jti 的访问 JWT 和刷新 JWT，并将刷新 JWT 保存到 Redis。
   *
   * @param authUserId 认证账号 ID，写入 JWT subject
   * @param userId 业务用户 ID
   * @param account 账号
   * @param roleCode 角色编码
   * @param roleId 角色 ID
   * @return 访问令牌、刷新令牌、Bearer 类型和访问令牌有效秒数
   */
  private TokenVo issueTokens(
      Long authUserId, Long userId, String account, String roleCode, Long roleId) {
    String jti = UUID.randomUUID().toString().replace("-", "");
    String accessToken =
        jwtUtil.sign(
            authUserId, userId, account, roleCode, roleId, jti, JwtType.ACCESS, ttlSeconds * 1000);
    // 刷新令牌签发为JWT（含 userId/jti），refreshToken 可解析后直查 Redis 索引键
    String refreshToken =
        jwtUtil.sign(
            authUserId, userId, account, roleCode, roleId, jti, JwtType.REFRESH, refreshTtlSeconds);
    redisTemplate
        .opsForValue()
        .set(
            REFRESH_KEY_PREFIX + authUserId + ":" + jti,
            refreshToken,
            refreshTtlSeconds,
            TimeUnit.SECONDS);
    return new TokenVo(accessToken, refreshToken, "Bearer", ttlSeconds);
  }

  /**
   * 校验认证账号已关联业务用户 ID，并返回该 ID。
   *
   * @param po 认证账号
   * @return 业务用户 ID
   * @throws BizException 账号尚未关联业务用户档案时抛出
   */
  private Long requireUserProfileId(AuthUserPo po) {
    if (po.getUserId() == null) {
      throw new BizException(AuthCodeEnum.USER_PROFILE_MISSING);
    }
    return po.getUserId();
  }
}
