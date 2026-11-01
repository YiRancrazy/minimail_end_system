package com.yirancrazy.minimall.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.dto.RegisterDto;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家认证服务单元测试；覆盖登录凭据、退出撤销、密码修改、注册和当前身份查询。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MerchantAuthServiceImplTest {

    @Mock private AuthUserManager authUserManager;
    @Mock private AuthRoleManager authRoleManager;
    @Mock private AuthTokenBlacklistManager tokenBlacklistManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private MerchantAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service =
                new MerchantAuthServiceImpl(
                        authUserManager,
                        authRoleManager,
                        tokenBlacklistManager,
                        jwtUtil,
                        redisTemplate,
                        900_000L,
                        604_800_000L);
    }

    private AuthRolePo stubMerchantRole() {
        AuthRolePo role = new AuthRolePo();
        role.setId(2L);
        role.setRoleCode("MERCHANT");
        role.setRoleName("商家");
        when(authRoleManager.getById(2L)).thenReturn(role);
        return role;
    }

    /** 验证商家登录成功：账号类型为 2、启用且密码匹配，登录后预期返回 MERCHANT 角色双令牌. */
    @Test
    void testLoginSuccessReturnsTokens() {
        stubMerchantRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(100L);
        po.setAccount("shopowner");
        po.setAccountType(2);
        po.setRoleId(2L);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        when(jwtUtil.sign(
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        eq(JwtType.ACCESS),
                        eq(900_000L)))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVo vo = service.login(new LoginDto("shopowner", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil)
                .sign(
                        eq(100L),
                        eq(100L),
                        eq("shopowner"),
                        eq("MERCHANT"),
                        eq(2L),
                        anyString(),
                        eq(JwtType.ACCESS),
                        eq(900_000L));
    }

    /** 验证商家账号不存在时登录失败：前置账号查询为空，调用登录后预期抛出 BizException. */
    @Test
    void testLoginAccountNotFoundThrows() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.login(new LoginDto("nobody", "pass")));
    }

    /** 验证非商家账号登录商家入口被拒绝：前置账号类型为 1，调用登录后预期抛出角色不匹配异常. */
    @Test
    void testLoginRoleMismatchThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(1);
        po.setStatus(1);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDto("alice", "pass")));
    }

    /** 验证禁用商家账号不能登录：前置类型正确但状态为 0，调用登录后预期抛出账号禁用异常. */
    @Test
    void testLoginAccountDisabledThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(2);
        po.setStatus(0);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDto("shop", "pass")));
    }

    /** 验证商家错误密码被拒绝：前置账号启用但密码不匹配，调用登录后预期返回密码错误异常. */
    @Test
    void testLoginPasswordInvalidThrows() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(100L);
        po.setAccountType(2);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDto("shop", "wrongpass")));
    }

    /** 验证商家退出清理会话：前置存在刷新令牌，退出后预期删除刷新键并写入两类黑名单记录. */
    @Test
    void testSignOutDeletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:100:*")).thenReturn(Set.of("refresh:100:abc"));
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        service.signOut(100L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:100:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any(TimeUnit.class));
        verify(tokenBlacklistManager).save(any());
    }

    /** 验证商家修改密码成功：前置旧密码正确，修改后预期更新密码散列并删除全部刷新令牌. */
    @Test
    void testChangePasswordSuccessUpdatesAndInvalidatesRefresh() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(100L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(100L)).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:100:*")).thenReturn(Set.of("refresh:100:abc"));

        service.changePassword(100L, new ChangePasswordDto("oldpass", "newpass123"));

        verify(authUserManager).updateById(any(AuthUserPo.class));
        verify(redisTemplate).delete(Set.of("refresh:100:abc"));
    }

    /** 验证商家修改密码账号不存在：前置账号查询为空，调用修改后预期抛出 BizException. */
    @Test
    void testChangePasswordAccountNotFoundThrows() {
        when(authUserManager.getById(999L)).thenReturn(null);
        assertThrows(
                BizException.class,
                () -> service.changePassword(999L, new ChangePasswordDto("old", "newpass123")));
    }

    /** 验证商家旧密码错误拒绝修改：前置密码散列不匹配，调用修改后预期抛出 BizException. */
    @Test
    void testChangePasswordOldPwdInvalidThrows() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(100L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(100L)).thenReturn(po);
        assertThrows(
                BizException.class,
                () -> service.changePassword(100L, new ChangePasswordDto("wrongpass", "newpass123")));
    }

    /** 验证商家注册成功：前置账号未占用且保存成功，注册后预期返回 Bearer 双令牌. */
    @Test
    void testRegisterSuccessReturnsTokens() {
        stubMerchantRole();
        when(authUserManager.count(any())).thenReturn(0L);
        when(authUserManager.save(any(AuthUserPo.class)))
                .thenAnswer(
                        invocation -> {
                            AuthUserPo po = invocation.getArgument(0);
                            po.setId(200L);
                            return true;
                        });
        when(jwtUtil.sign(
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        eq(JwtType.ACCESS),
                        eq(900_000L)))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVo vo = service.register(new RegisterDto("shopowner", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
        verify(authUserManager).save(any(AuthUserPo.class));
    }

    /** 验证重复商家账号被拒绝：前置账号计数为 1，调用注册后预期抛出商家已存在异常. */
    @Test
    void testRegisterDuplicateThrows() {
        when(authUserManager.count(any())).thenReturn(1L);

        BizException ex =
                assertThrows(
                        BizException.class, () -> service.register(new RegisterDto("shopowner", "pass123")));
        assertEquals(AuthCodeEnum.MERCHANT_EXISTS.getCode(), ex.getCode());
    }

    /** 验证有效 JWT 查询商家信息：前置声明含账号和 MERCHANT 角色，调用后预期账号 ID 兼作业务用户 ID. */
    @Test
    void testMeValidTokenReturnsUserInfo() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("100");
        when(claims.get("account", String.class)).thenReturn("shopowner");
        when(claims.get("role", String.class)).thenReturn("MERCHANT");
        when(claims.get("roleId", Long.class)).thenReturn(2L);
        when(jwtUtil.parse("access-token")).thenReturn(claims);

        UserInfoVo vo = service.me("access-token");

        assertEquals(100L, vo.getUserId());
        assertEquals("shopowner", vo.getAccount());
        assertEquals("MERCHANT", vo.getRole());
        assertEquals(2L, vo.getRoleId());
    }

    /** 验证无效 JWT 查询商家信息失败：前置解析抛异常，调用 me 后预期转换为令牌无效错误. */
    @Test
    void testMeInvalidTokenThrows() {
        when(jwtUtil.parse("bad-token")).thenThrow(new JwtException("expired"));

        BizException ex = assertThrows(BizException.class, () -> service.me("bad-token"));
        assertEquals(AuthCodeEnum.TOKEN_INVALID.getCode(), ex.getCode());
    }
}
