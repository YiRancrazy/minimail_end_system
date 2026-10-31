package com.yirancrazy.minimall.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthTokenBlacklistPo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.service.MerchantAuthService;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家认证核心实现；限定账号类型为商家，负责密码校验、JWT 签发、刷新令牌缓存和撤销。
 * 商家账号保存在 MySQL，刷新令牌和撤销缓存保存在 Redis，不依赖普通用户的远程档案创建流程。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
  public MerchantAuthServiceImpl(
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
   * 注册商家账号：确认账号未被占用后，创建类型 2、启用状态和默认商家角色的认证账号。
   * 密码使用随机盐和 BCrypt 散列；注册成功后签发访问 JWT 和随机刷新令牌。
   *
   * @param dto 商家注册账号和明文密码
   * @return 新签发的访问令牌和刷新令牌
   * @throws BizException 账号已存在时抛出
   */
  @Override
  public TokenVo register(RegisterDto dto) {
    long existing =
        authUserManager.count(
            Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
    if (existing > 0) {
      throw new BizException(AuthCodeEnum.MERCHANT_EXISTS);
    }
    String salt = UUID.randomUUID().toString().replace("-", "");
    String hash = BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt(BCRYPT_COST));
    AuthUserPo po = new AuthUserPo();
    po.setAccount(dto.getAccount());
    po.setPasswordHash(hash);
    po.setSalt(salt);
    po.setAccountType(ACCOUNT_TYPE_MERCHANT);
    po.setRoleId(DEFAULT_ROLE_ID_MERCHANT);
    po.setStatus(1);
    authUserManager.save(po);
    log.info("商家已注册，accountId={}，account={}", po.getId(), po.getAccount());
    return issueTokens(po.getId(), po.getAccount(), po.getRoleId());
  }

  /**
   * 校验商家账号类型、启用状态和 BCrypt 密码，成功后更新最近登录时间并签发令牌。
   * 当前流程不包含普通用户登录使用的 Redis 失败次数锁定。
   *
   * @param dto 商家登录账号和明文密码
   * @return 新签发的访问令牌和刷新令牌
   * @throws BizException 账号不存在、类型不匹配、禁用或密码错误时抛出
   */
  @Override
  public TokenVo login(LoginDto dto) {
    AuthUserPo po =
        authUserManager.getOne(
            Wrappers.lambdaQuery(AuthUserPo.class).eq(AuthUserPo::getAccount, dto.getAccount()));
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
    log.info("商家已登录，accountId={}，account={}", po.getId(), po.getAccount());
    return issueTokens(po.getId(), po.getAccount(), po.getRoleId());
  }

  /**
   * 退出商家会话：删除该账号全部刷新令牌，并将当前 jti 写入数据库黑名单和 Redis。
   *
   * @param merchantAccountId 商家认证账号 ID
   * @param jti 当前 JWT 唯一标识
   */
  public void signOut(Long merchantAccountId, String jti) {
    var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + merchantAccountId + ":*");
    if (keys != null) {
      redisTemplate.delete(keys);
    }
    revokeToken(jti, merchantAccountId, "sign_out");
    log.info("商家已退出登录，accountId={}", merchantAccountId);
  }

  /**
   * 校验旧密码后更新商家密码，并删除该账号的全部刷新令牌。
   *
   * @param merchantAccountId 商家认证账号 ID
   * @param dto 旧密码和新密码
   * @throws BizException 账号不存在或旧密码错误时抛出
   */
  public void changePassword(Long merchantAccountId, ChangePasswordDto dto) {
    AuthUserPo po = authUserManager.getById(merchantAccountId);
    if (po == null) {
      throw new BizException(AuthCodeEnum.MERCHANT_NOT_FOUND);
    }
    if (!BCrypt.checkpw(dto.getOldPassword() + po.getSalt(), po.getPasswordHash())) {
      throw new BizException(AuthCodeEnum.PWD_INVALID);
    }
    applyNewPassword(po, dto.getNewPassword());
    invalidateRefreshTokens(merchantAccountId);
    log.info("商家密码已修改，accountId={}", merchantAccountId);
  }

  /**
   * 校验 JWT 并从声明中恢复商家身份；商家没有独立业务用户 ID，userId 等于认证账号 ID。
   *
   * @param token 不带 Bearer 前缀的 JWT
   * @return 商家认证账号、账号和角色信息
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
    return new UserInfoVo(
        authUserId,
        authUserId,
        c.get("account", String.class),
        c.get("role", String.class),
        c.get("roleId", Long.class));
  }

  /**
   * 使用新的随机盐和 BCrypt 成本 12 生成密码散列，并持久化商家账号。
   *
   * @param po 待更新的商家账号
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
   * 删除指定商家账号在 Redis 中以 refresh:{merchantAccountId}: 开头的全部刷新令牌。
   *
   * @param merchantAccountId 商家认证账号 ID
   */
  private void invalidateRefreshTokens(Long merchantAccountId) {
    var keys = redisTemplate.keys(REFRESH_KEY_PREFIX + merchantAccountId + ":*");
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  /**
   * 将指定 jti 写入数据库黑名单和 Redis；缓存有效时长与访问令牌一致。
   *
   * @param jti JWT 唯一标识
   * @param userId 商家认证账号 ID
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
   * 根据角色 ID 查询角色编码；角色 ID 为空或角色不存在时回退为 MERCHANT。
   *
   * @param roleId 持久化角色 ID，可为空
   * @return 可写入 JWT 的角色编码
   */
  private String resolveRoleCode(Long roleId) {
    if (roleId == null) {
      return "MERCHANT";
    }
    AuthRolePo role = authRoleManager.getById(roleId);
    return role != null ? role.getRoleCode() : "MERCHANT";
  }

  /**
   * 签发商家访问 JWT 和随机刷新令牌，并将刷新令牌保存到 Redis。
   * 访问 JWT 的认证账号 ID 与业务用户 ID 均使用商家认证账号 ID。
   *
   * @param accountId 商家认证账号 ID
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
