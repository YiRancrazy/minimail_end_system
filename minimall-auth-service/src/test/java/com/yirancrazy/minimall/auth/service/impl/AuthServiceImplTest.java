package com.yirancrazy.minimall.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDto;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDto;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.gateway.AuthRemoteGateway;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 普通用户认证服务单元测试；覆盖注册、登录锁定、刷新轮换、退出、密码修改和重置流程。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private AuthUserManager authUserManager;
    @Mock private AuthRoleManager authRoleManager;
    @Mock private AuthTokenBlacklistManager tokenBlacklistManager;
    @Mock private AuthRemoteGateway authRemoteGateway;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authService =
                new AuthServiceImpl(
                        authUserManager,
                        authRoleManager,
                        tokenBlacklistManager,
                        authRemoteGateway,
                        jwtUtil,
                        redisTemplate,
                        900_000L,
                        604_800_000L);
    }

    private AuthRolePo stubUserRole() {
        AuthRolePo role = new AuthRolePo();
        role.setId(1L);
        role.setRoleCode("USER");
        role.setRoleName("普通用户");
        when(authRoleManager.getById(1L)).thenReturn(role);
        return role;
    }

    /** 验证注册成功：账号不存在且远程档案创建成功，执行注册后预期返回 Bearer 双令牌. */
    @Test
    void testRegisterSuccess() {
        stubUserRole();
        when(authUserManager.getOne(any())).thenReturn(null);
        when(authRemoteGateway.nextUserId()).thenReturn(9001L);
        when(authUserManager.save(any(AuthUserPo.class)))
                .thenAnswer(
                        invocation -> {
                            AuthUserPo po = invocation.getArgument(0);
                            po.setId(42L);
                            return true;
                        });
        stubIssuedTokens("access-token", "refresh-token");

        TokenVo vo = authService.register(new RegisterDto("testuser", "pass123"));
        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
        verify(authRemoteGateway).provisionUserProfile(9001L);
    }

    /** 验证重复账号注册被拒绝：前置账号已存在，调用注册后预期抛出 BizException 且不继续创建. */
    @Test
    void testRegisterUserExistsThrows() {
        when(authUserManager.getOne(any())).thenReturn(new AuthUserPo());
        assertThrows(BizException.class, () -> authService.register(new RegisterDto("exists", "pass")));
    }

    /** 验证档案创建失败回滚注册：前置远程档案创建抛错，执行注册后预期抛错且认证账号从未保存. */
    @Test
    void testRegisterProfileFailureDoesNotSaveAuthAccount() {
        when(authUserManager.getOne(any())).thenReturn(null);
        when(authRemoteGateway.nextUserId()).thenReturn(9001L);
        doThrow(new BizException(AuthCodeEnum.USER_PROFILE_CREATE_FAIL))
                .when(authRemoteGateway)
                .provisionUserProfile(9001L);

        assertThrows(
                BizException.class, () -> authService.register(new RegisterDto("testuser", "pass123")));
        verify(authUserManager, never()).save(any(AuthUserPo.class));
    }

    /** 验证普通用户不存在时登录失败：前置账号查询为空，调用登录后预期抛出 BizException. */
    @Test
    void testLoginUserNotFoundThrows() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> authService.login(new LoginDto("nobody", "pass")));
    }

    /** 验证普通用户登录成功：账号类型为 1、启用且密码匹配，登录后预期返回 USER 角色双令牌. */
    @Test
    void testLoginUserAccountSucceeds() {
        stubUserRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("alice");
        po.setAccountType(1);
        po.setUserId(9001L);
        po.setRoleId(1L);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        stubIssuedTokens("access-token", "refresh-token");

        TokenVo vo = authService.login(new LoginDto("alice", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil)
                .sign(
                        eq(1L),
                        eq(9001L),
                        eq("alice"),
                        eq("USER"),
                        eq(1L),
                        anyString(),
                        eq(JwtType.ACCESS),
                        eq(900_000L));
    }

    /** 验证平台账号登录普通入口被拒绝：前置账号类型为 3，调用登录后预期抛出角色不匹配异常. */
    @Test
    void testLoginPlatformAccountRejected() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(3);
        po.setStatus(1);
        when(authUserManager.getOne(any())).thenReturn(po);

        BizException ex =
                assertThrows(BizException.class, () -> authService.login(new LoginDto("admin", "pass")));
        assertEquals(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH.getCode(), ex.getCode());
    }

    /** 验证禁用普通账号不能登录：前置类型正确但状态为 0，调用登录后预期抛出账号禁用异常. */
    @Test
    void testLoginDisabledAccountRejected() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(1);
        po.setStatus(0);
        when(authUserManager.getOne(any())).thenReturn(po);

        BizException ex =
                assertThrows(BizException.class, () -> authService.login(new LoginDto("alice", "pass")));
        assertEquals(AuthCodeEnum.ACCOUNT_DISABLED.getCode(), ex.getCode());
    }

    /** 验证错误密码被拒绝：前置账号启用但密码不匹配，执行登录后预期返回密码错误业务异常. */
    @Test
    void testLoginWrongPasswordStillThrows() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccountType(1);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);

        BizException ex =
                assertThrows(
                        BizException.class, () -> authService.login(new LoginDto("alice", "wrongpass")));
        assertEquals(AuthCodeEnum.PWD_INVALID.getCode(), ex.getCode());
    }

    /** 验证无效刷新令牌被拒绝：前置 JWT 解析抛出异常，调用刷新后预期返回刷新令牌无效错误. */
    @Test
    void testRefreshTokenInvalidThrows() {
        when(jwtUtil.parse(anyString())).thenThrow(new JwtException("bad token"));
        assertThrows(BizException.class, () -> authService.refreshToken("bad-refresh"));
    }

    /** 验证刷新令牌原子消费：前置索引删除成功，执行刷新后预期返回新令牌并撤销旧 jti. */
    @Test
    void testRefreshTokenResolvesByIndexedKey() {
        stubUserRole();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("jti", String.class)).thenReturn("abc");
        when(claims.get("type", String.class)).thenReturn(JwtType.REFRESH);
        when(jwtUtil.parse("refresh-jwt")).thenReturn(claims);
        // 索引键 refresh:1:abc 命中且原子比较删除成功，才允许轮换
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        stubIssuedTokens("new-access", "new-refresh");
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("alice");
        po.setRoleId(1L);
        po.setUserId(9001L);
        when(authUserManager.getById(1L)).thenReturn(po);

        TokenVo vo = authService.refreshToken("refresh-jwt");

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(redisTemplate)
                .execute(any(RedisScript.class), eq(List.of("refresh:1:abc")), eq("refresh-jwt"));
        verify(tokenBlacklistManager).save(any());
    }

    /** 验证刷新令牌重放被拒绝：前置原子删除返回 0，调用刷新后预期抛错且不再签发令牌. */
    @Test
    void testRefreshTokenAtomicDeleteLostThrows() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("jti", String.class)).thenReturn("abc");
        when(claims.get("type", String.class)).thenReturn(JwtType.REFRESH);
        when(jwtUtil.parse("refresh-jwt")).thenReturn(claims);
        // 比较并删除返回 0：键不存在或值不匹配（旧 token 重放），视为无效
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);

        assertThrows(BizException.class, () -> authService.refreshToken("refresh-jwt"));
        verify(jwtUtil, never())
                .sign(
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong());
    }

    /** 验证访问令牌不能刷新：前置 JWT 类型为 ACCESS，调用刷新后预期报错且不访问 Redis. */
    @Test
    void testRefreshTokenAccessTypeThrows() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("jti", String.class)).thenReturn("abc");
        when(claims.get("type", String.class)).thenReturn(JwtType.ACCESS);
        when(jwtUtil.parse("access-jwt")).thenReturn(claims);

        BizException ex =
                assertThrows(BizException.class, () -> authService.refreshToken("access-jwt"));

        assertEquals(AuthCodeEnum.REFRESH_TOKEN_INVALID.getCode(), ex.getCode());
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
    }

    /** 验证退出清理会话：前置存在刷新令牌，退出后预期删除刷新键并写入数据库和 Redis 黑名单. */
    @Test
    void testSignOutDeletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        authService.signOut(1L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any());
        verify(tokenBlacklistManager).save(any());
    }

    /** 验证修改密码成功：前置旧密码正确，修改后预期更新密码散列并删除全部刷新令牌. */
    @Test
    void testChangePasswordSuccessUpdatesAndInvalidatesRefresh() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(1L)).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));

        authService.changePassword(1L, new ChangePasswordDto("oldpass", "newpass123"));

        verify(authUserManager).updateById(any(AuthUserPo.class));
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
    }

    /** 验证修改密码账号不存在：前置账号查询为空，调用修改后预期抛出 BizException. */
    @Test
    void testChangePasswordUserNotFoundThrows() {
        when(authUserManager.getById(999L)).thenReturn(null);
        assertThrows(
                BizException.class,
                () -> authService.changePassword(999L, new ChangePasswordDto("old", "newpass123")));
    }

    /** 验证旧密码错误拒绝修改：前置密码散列不匹配，调用修改后预期抛出 BizException. */
    @Test
    void testChangePasswordOldPwdInvalidThrows() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(1L)).thenReturn(po);

        assertThrows(
                BizException.class,
                () -> authService.changePassword(1L, new ChangePasswordDto("wrongpass", "newpass123")));
    }

    /** 验证重置码生成成功：前置账号存在且冷却键占用成功，发送后预期写入 600 秒验证码. */
    @Test
    void testSendResetCodeSuccessStoresCode() {
        AuthUserPo po = new AuthUserPo();
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.setIfAbsent(eq("pwd:reset:cooldown:testuser"), eq("1"), eq(60L), any()))
                .thenReturn(true);

        authService.sendResetCode(new SendResetCodeDto("testuser"));

        verify(valueOperations).set(eq("pwd:reset:code:testuser"), anyString(), eq(600L), any());
    }

    /** 验证冷却期阻止重复申请：前置冷却键已存在，发送后预期频繁错误且不覆盖验证码. */
    @Test
    void testSendResetCodeCooldownBlocksRepeatWithin60s() {
        AuthUserPo po = new AuthUserPo();
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        // 冷却键已存在（60s 窗口内第二次发送），setIfAbsent 返回 false
        when(valueOperations.setIfAbsent(eq("pwd:reset:cooldown:testuser"), eq("1"), eq(60L), any()))
                .thenReturn(false);

        BizException ex =
                assertThrows(
                        BizException.class, () -> authService.sendResetCode(new SendResetCodeDto("testuser")));

        assertEquals(AuthCodeEnum.RESET_CODE_TOO_FREQUENT.getCode(), ex.getCode());
        verify(valueOperations, never())
                .set(eq("pwd:reset:code:testuser"), anyString(), anyLong(), any());
    }

    /** 验证冷却期结束可再次申请：前置冷却键占用成功，调用发送后预期正常完成且不抛异常. */
    @Test
    void testSendResetCodeOkAfterNoCooldown() {
        AuthUserPo po = new AuthUserPo();
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.setIfAbsent(eq("pwd:reset:cooldown:testuser"), eq("1"), eq(60L), any()))
                .thenReturn(true);

        authService.sendResetCode(new SendResetCodeDto("testuser"));
    }

    /** 验证不存在账号不能申请重置码：前置账号查询为空，调用发送后预期抛错且不生成验证码. */
    @Test
    void testSendResetCodeUserNotFoundThrows() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(
                BizException.class, () -> authService.sendResetCode(new SendResetCodeDto("nobody")));
    }

    /** 验证重置密码成功：前置验证码匹配，执行后预期更新密码、删除验证码并清理刷新令牌. */
    @Test
    void testResetPasswordSuccessUpdatesAndClearsCodeAndRefresh() {
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn("123456");
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));

        authService.resetPassword(new ResetPasswordDto("testuser", "123456", "newpass123"));

        verify(authUserManager).updateById(any(AuthUserPo.class));
        verify(redisTemplate).delete("pwd:reset:code:testuser");
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
    }

    /** 验证重置密码账号不存在：前置账号查询为空，调用后预期抛出 BizException 且不处理验证码. */
    @Test
    void testResetPasswordUserNotFoundThrows() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(
                BizException.class,
                () -> authService.resetPassword(new ResetPasswordDto("nobody", "123456", "newpass123")));
    }

    /** 验证重置码无效被拒绝：前置账号存在但缓存无验证码，调用后预期抛错且不更新密码. */
    @Test
    void testResetPasswordVerifyCodeInvalidThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn(null);

        assertThrows(
                BizException.class,
                () -> authService.resetPassword(new ResetPasswordDto("testuser", "123456", "newpass123")));
    }

    /** 验证验证码尝试超限：前置失败计数为 5，即使验证码正确，调用后仍预期抛错且不更新密码. */
    @Test
    void testResetPasswordVerifyCodeFailsAfterMaxAttempts() {
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        // 失败计数已到阈值 5，即使验证码正确也拒绝
        when(valueOperations.get("pwd:reset:fail:testuser")).thenReturn("5");
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn("123456");

        BizException ex =
                assertThrows(
                        BizException.class,
                        () ->
                                authService.resetPassword(
                                        new ResetPasswordDto("testuser", "123456", "newpass123")));

        assertEquals(AuthCodeEnum.VERIFY_CODE_ATTEMPT_EXCEEDED.getCode(), ex.getCode());
        verify(authUserManager, never()).updateById(any(AuthUserPo.class));
    }

    /** 验证重置成功清除失败计数：前置已失败 3 次且验证码正确，执行后预期删除计数和验证码. */
    @Test
    void testResetPasswordSuccessClearsFailCounter() {
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        // 此前已失败 3 次，本次验证码正确，应清除失败计数并重置密码
        when(valueOperations.get("pwd:reset:fail:testuser")).thenReturn("3");
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn("123456");
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));

        authService.resetPassword(new ResetPasswordDto("testuser", "123456", "newpass123"));

        verify(redisTemplate).delete("pwd:reset:fail:testuser");
        verify(redisTemplate).delete("pwd:reset:code:testuser");
    }

    /** 验证登录失败次数超限：前置 Redis 计数为 5，调用登录后预期直接抛错且不再校验密码. */
    @Test
    void testLoginAttemptExceededThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(1);
        po.setStatus(1);
        when(authUserManager.getOne(any())).thenReturn(po);
        // 失败计数已达阈值 5，直接拒绝登录
        when(valueOperations.get("login:fail:alice")).thenReturn("5");

        BizException ex =
                assertThrows(BizException.class, () -> authService.login(new LoginDto("alice", "pass123")));
        assertEquals(AuthCodeEnum.LOGIN_ATTEMPT_EXCEEDED.getCode(), ex.getCode());
    }

    /** 验证错误密码累计次数：前置账户有效但密码错误，登录后预期计数递增并设置 600 秒窗口. */
    @Test
    void testLoginWrongPasswordIncrementsFailCounter() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("alice");
        po.setAccountType(1);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);

        assertThrows(BizException.class, () -> authService.login(new LoginDto("alice", "wrongpass")));

        verify(valueOperations).increment("login:fail:alice");
        verify(redisTemplate).expire("login:fail:alice", 600L, TimeUnit.SECONDS);
    }

    /** 验证登录成功清除失败次数：前置计数为 3 且凭据正确，登录后预期删除失败键并签发令牌. */
    @Test
    void testLoginSuccessClearsFailCounter() {
        stubUserRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(1L);
        po.setAccount("alice");
        po.setAccountType(1);
        po.setRoleId(1L);
        po.setStatus(1);
        po.setUserId(9001L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        when(valueOperations.get("login:fail:alice")).thenReturn("3");
        stubIssuedTokens("access-token", "refresh-token");

        authService.login(new LoginDto("alice", "pass123"));

        verify(redisTemplate).delete("login:fail:alice");
    }

    /** 验证注册密码长度约束：前置密码为 5 位，校验后预期 password 字段产生一条违规. */
    @Test
    void testRegisterPasswordTooShortRejected() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<RegisterDto>> violations =
                validator.validate(new RegisterDto("testuser", "12345"));
        assertEquals(1, violations.size());
        assertEquals("password", violations.iterator().next().getPropertyPath().toString());
    }

    private void stubIssuedTokens(String accessToken, String refreshToken) {
        when(jwtUtil.sign(
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        eq(JwtType.ACCESS),
                        anyLong()))
                .thenReturn(accessToken);
        when(jwtUtil.sign(
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        eq(JwtType.REFRESH),
                        anyLong()))
                .thenReturn(refreshToken);
    }
}
