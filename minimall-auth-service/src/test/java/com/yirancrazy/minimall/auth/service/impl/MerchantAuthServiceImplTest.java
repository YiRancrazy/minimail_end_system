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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
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
 * @Description: MerchantAuthServiceImpl 的单元测试类，覆盖注册/登录/登出/修改密码的正常与失败场景。
 * @Version: 2.0
 * @DateTime: 2026/08/04
 **/
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
        service = new MerchantAuthServiceImpl(
            authUserManager, authRoleManager, tokenBlacklistManager,
            jwtUtil, redisTemplate, 900L, 604800L);
    }

    private AuthRolePO stubMerchantRole() {
        AuthRolePO role = new AuthRolePO();
        role.setId(2L);
        role.setRoleCode("MERCHANT");
        role.setRoleName("商家");
        when(authRoleManager.getById(2L)).thenReturn(role);
        return role;
    }

    @Test
    void login_success_returnsTokens() {
        stubMerchantRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(100L);
        po.setAccount("shopowner");
        po.setAccountType(2);
        po.setRoleId(2L);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = service.login(new LoginDTO("shopowner", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil).sign(eq(100L), eq("shopowner"), eq("MERCHANT"), eq(2L), anyString());
    }

    @Test
    void login_accountNotFound_throws() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("nobody", "pass")));
    }

    @Test
    void login_roleMismatch_throws() {
        AuthUserPO po = new AuthUserPO();
        po.setAccountType(1);
        po.setStatus(1);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("alice", "pass")));
    }

    @Test
    void login_accountDisabled_throws() {
        AuthUserPO po = new AuthUserPO();
        po.setAccountType(2);
        po.setStatus(0);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("shop", "pass")));
    }

    @Test
    void login_passwordInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(100L);
        po.setAccountType(2);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("shop", "wrongpass")));
    }

    @Test
    void signOut_deletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:100:*")).thenReturn(Set.of("refresh:100:abc"));
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        service.signOut(100L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:100:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any(TimeUnit.class));
        verify(tokenBlacklistManager).save(any());
    }

    @Test
    void changePassword_success_updatesAndInvalidatesRefresh() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(100L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(100L)).thenReturn(po);
        when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:100:*")).thenReturn(Set.of("refresh:100:abc"));

        service.changePassword(100L, new ChangePasswordDTO("oldpass", "newpass123"));

        verify(authUserManager).updateById(any(AuthUserPO.class));
        verify(redisTemplate).delete(Set.of("refresh:100:abc"));
    }

    @Test
    void changePassword_accountNotFound_throws() {
        when(authUserManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.changePassword(999L, new ChangePasswordDTO("old", "newpass123")));
    }

    @Test
    void changePassword_oldPwdInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(100L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getById(100L)).thenReturn(po);
        assertThrows(BizException.class,
            () -> service.changePassword(100L, new ChangePasswordDTO("wrongpass", "newpass123")));
    }

    @Test
    void register_success_returnsTokens() {
        stubMerchantRole();
        when(authUserManager.count(any())).thenReturn(0L);
        when(authUserManager.save(any(AuthUserPO.class))).thenAnswer(invocation -> {
            AuthUserPO po = invocation.getArgument(0);
            po.setId(200L);
            return true;
        });
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = service.register(new RegisterDTO("shopowner", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
        verify(authUserManager).save(any(AuthUserPO.class));
    }

    @Test
    void register_duplicate_throws() {
        when(authUserManager.count(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
            () -> service.register(new RegisterDTO("shopowner", "pass123")));
        assertEquals(AuthCodeEnum.MERCHANT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void me_validToken_returnsUserInfo() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("100");
        when(claims.get("account", String.class)).thenReturn("shopowner");
        when(claims.get("role", String.class)).thenReturn("MERCHANT");
        when(claims.get("roleId", Long.class)).thenReturn(2L);
        when(jwtUtil.parse("access-token")).thenReturn(claims);

        UserInfoVO vo = service.me("access-token");

        assertEquals(100L, vo.getUserId());
        assertEquals("shopowner", vo.getAccount());
        assertEquals("MERCHANT", vo.getRole());
        assertEquals(2L, vo.getRoleId());
    }

    @Test
    void me_invalidToken_throws() {
        when(jwtUtil.parse("bad-token")).thenThrow(new JwtException("expired"));

        BizException ex = assertThrows(BizException.class, () -> service.me("bad-token"));
        assertEquals(AuthCodeEnum.TOKEN_INVALID.getCode(), ex.getCode());
    }
}
