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

        authService.sendResetCode(new SendResetCodeDTO("testuser"));

        verify(valueOperations).set(eq("pwd:reset:code:testuser"), anyString(), eq(600L), any());
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
}
