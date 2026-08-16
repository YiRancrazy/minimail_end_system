package com.yirancrazy.minimall.auth.service.impl;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.dto.ResetPasswordDTO;
import com.yirancrazy.minimall.auth.dto.SendResetCodeDTO;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: AuthServiceImpl 的单元测试类。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private AuthUserManager authUserManager;
    @Mock private AuthRoleManager authRoleManager;
    @Mock private AuthTokenBlacklistManager tokenBlacklistManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthServiceImpl(
            authUserManager, authRoleManager, tokenBlacklistManager,
            jwtUtil, redisTemplate, 900L, 604800L);
    }

    private AuthRolePO stubUserRole() {
        AuthRolePO role = new AuthRolePO();
        role.setId(1L);
        role.setRoleCode("USER");
        role.setRoleName("普通用户");
        when(authRoleManager.getById(1L)).thenReturn(role);
        return role;
    }

    @Test
    void register_success() {
        stubUserRole();
        when(authUserManager.getOne(any())).thenReturn(null);
        when(authUserManager.save(any(AuthUserPO.class))).thenAnswer(invocation -> {
            AuthUserPO po = invocation.getArgument(0);
            po.setId(42L);
            return true;
        });
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = authService.register(new RegisterDTO("testuser", "pass123"));
        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
    }

    @Test
    void register_userExists_throws() {
        when(authUserManager.getOne(any())).thenReturn(new AuthUserPO());
        assertThrows(BizException.class, () -> authService.register(new RegisterDTO("exists", "pass")));
    }

    @Test
    void login_userNotFound_throws() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> authService.login(new LoginDTO("nobody", "pass")));
    }

    @Test
    void login_user_account_succeeds() {
        stubUserRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccount("alice");
        po.setAccountType(1);
        po.setRoleId(1L);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = authService.login(new LoginDTO("alice", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil).sign(eq(1L), eq("alice"), eq("USER"), eq(1L), anyString());
    }

    @Test
    void login_platform_account_rejected() {
        AuthUserPO po = new AuthUserPO();
        po.setAccountType(3);
        po.setStatus(1);
        when(authUserManager.getOne(any())).thenReturn(po);

        BizException ex = assertThrows(BizException.class,
            () -> authService.login(new LoginDTO("admin", "pass")));
        assertEquals(AuthCodeEnum.ACCOUNT_ROLE_MISMATCH.getCode(), ex.getCode());
    }

    @Test
    void login_disabled_account_rejected() {
        AuthUserPO po = new AuthUserPO();
        po.setAccountType(1);
        po.setStatus(0);
        when(authUserManager.getOne(any())).thenReturn(po);

        BizException ex = assertThrows(BizException.class,
            () -> authService.login(new LoginDTO("alice", "pass")));
        assertEquals(AuthCodeEnum.ACCOUNT_DISABLED.getCode(), ex.getCode());
    }

    @Test
    void login_wrong_password_still_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccountType(1);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);

        BizException ex = assertThrows(BizException.class,
            () -> authService.login(new LoginDTO("alice", "wrongpass")));
        assertEquals(AuthCodeEnum.PWD_INVALID.getCode(), ex.getCode());
    }

    @Test
    void refreshToken_invalid_throws() {
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());
        assertThrows(BizException.class, () -> authService.refreshToken("bad-refresh"));
    }

    @Test
    void signOut_deletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        authService.signOut(1L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any());
        verify(tokenBlacklistManager).save(any());
    }

    @Test
    void changePassword_success_updatesAndInvalidatesRefresh() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(1L)).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));

        authService.changePassword(1L, new ChangePasswordDTO("oldpass", "newpass123"));

        verify(authUserManager).updateById(any(AuthUserPO.class));
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
    }

    @Test
    void changePassword_userNotFound_throws() {
        when(authUserManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> authService.changePassword(999L, new ChangePasswordDTO("old", "newpass123")));
    }

    @Test
    void changePassword_oldPwdInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(1L)).thenReturn(po);

        assertThrows(BizException.class,
            () -> authService.changePassword(1L, new ChangePasswordDTO("wrongpass", "newpass123")));
    }

    @Test
    void sendResetCode_success_storesCode() {
        AuthUserPO po = new AuthUserPO();
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.setIfAbsent(eq("pwd:reset:cooldown:testuser"), eq("1"), eq(60L), any()))
            .thenReturn(true);

        authService.sendResetCode(new SendResetCodeDTO("testuser"));

        verify(valueOperations).set(eq("pwd:reset:code:testuser"), anyString(), eq(600L), any());
    }

    @Test
    void sendResetCode_cooldown_blocks_repeat_within_60s() {
        AuthUserPO po = new AuthUserPO();
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        // 冷却键已存在（60s 窗口内第二次发送），setIfAbsent 返回 false
        when(valueOperations.setIfAbsent(eq("pwd:reset:cooldown:testuser"), eq("1"), eq(60L), any()))
            .thenReturn(false);

        BizException ex = assertThrows(BizException.class,
            () -> authService.sendResetCode(new SendResetCodeDTO("testuser")));

        assertEquals(AuthCodeEnum.RESET_CODE_TOO_FREQUENT.getCode(), ex.getCode());
        verify(valueOperations, never()).set(eq("pwd:reset:code:testuser"), anyString(), anyLong(), any());
    }

    @Test
    void sendResetCode_ok_after_no_cooldown() {
        AuthUserPO po = new AuthUserPO();
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.setIfAbsent(eq("pwd:reset:cooldown:testuser"), eq("1"), eq(60L), any()))
            .thenReturn(true);

        authService.sendResetCode(new SendResetCodeDTO("testuser"));
    }

    @Test
    void sendResetCode_userNotFound_throws() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class,
            () -> authService.sendResetCode(new SendResetCodeDTO("nobody")));
    }

    @Test
    void resetPassword_success_updatesAndClearsCodeAndRefresh() {
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn("123456");
        when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));

        authService.resetPassword(new ResetPasswordDTO("testuser", "123456", "newpass123"));

        verify(authUserManager).updateById(any(AuthUserPO.class));
        verify(redisTemplate).delete("pwd:reset:code:testuser");
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
    }

    @Test
    void resetPassword_userNotFound_throws() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class,
            () -> authService.resetPassword(new ResetPasswordDTO("nobody", "123456", "newpass123")));
    }

    @Test
    void resetPassword_verifyCodeInvalid_throws() {
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn(null);

        assertThrows(BizException.class,
            () -> authService.resetPassword(new ResetPasswordDTO("testuser", "123456", "newpass123")));
    }

    @Test
    void resetPassword_verify_code_fails_after_max_attempts() {
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        // 失败计数已到阈值 5，即使验证码正确也拒绝
        when(valueOperations.get("pwd:reset:fail:testuser")).thenReturn("5");
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn("123456");

        BizException ex = assertThrows(BizException.class,
            () -> authService.resetPassword(new ResetPasswordDTO("testuser", "123456", "newpass123")));

        assertEquals(AuthCodeEnum.VERIFY_CODE_ATTEMPT_EXCEEDED.getCode(), ex.getCode());
        verify(authUserManager, never()).updateById(any(AuthUserPO.class));
    }

    @Test
    void resetPassword_success_clears_fail_counter() {
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccount("testuser");
        when(authUserManager.getOne(any())).thenReturn(po);
        // 此前已失败 3 次，本次验证码正确，应清除失败计数并重置密码
        when(valueOperations.get("pwd:reset:fail:testuser")).thenReturn("3");
        when(valueOperations.get("pwd:reset:code:testuser")).thenReturn("123456");
        when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:1:*")).thenReturn(Set.of("refresh:1:abc"));

        authService.resetPassword(new ResetPasswordDTO("testuser", "123456", "newpass123"));

        verify(redisTemplate).delete("pwd:reset:fail:testuser");
        verify(redisTemplate).delete("pwd:reset:code:testuser");
    }
}
