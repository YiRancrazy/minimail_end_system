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
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantAuthServiceImpl 的单元测试类，覆盖注册/登录/登出/修改密码的正常与失败场景。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MerchantAuthServiceImplTest {

    @Mock private UserAuthManager userAuthManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private MerchantAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new MerchantAuthServiceImpl(userAuthManager, jwtUtil, redisTemplate, 900L, 604800L);
    }

    @Test
    void login_success_returnsTokens() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setId(100L);
        po.setUsername("shopowner");
        po.setRole("MERCHANT");
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(userAuthManager.getOne(any())).thenReturn(po);
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = service.login(new LoginDTO("shopowner", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil).sign(eq(100L), eq("shopowner"), eq("MERCHANT"), anyString());
    }

    @Test
    void login_accountNotFound_throws() {
        when(userAuthManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("nobody", "pass")));
    }

    @Test
    void login_roleMismatch_throws() {
        UserAuthPO po = new UserAuthPO();
        po.setRole("USER");
        po.setStatus(1);
        when(userAuthManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("alice", "pass")));
    }

    @Test
    void login_accountDisabled_throws() {
        UserAuthPO po = new UserAuthPO();
        po.setRole("MERCHANT");
        po.setStatus(0);
        when(userAuthManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("shop", "pass")));
    }

    @Test
    void login_passwordInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setId(100L);
        po.setRole("MERCHANT");
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(userAuthManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("shop", "wrongpass")));
    }

    @Test
    void signOut_deletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:100:*")).thenReturn(Set.of("refresh:100:abc"));
        service.signOut(100L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:100:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any(TimeUnit.class));
    }

    @Test
    void changePassword_success_updatesAndInvalidatesRefresh() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setId(100L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(userAuthManager.getById(100L)).thenReturn(po);
        when(userAuthManager.updateById(any(UserAuthPO.class))).thenReturn(true);
        when(redisTemplate.keys("refresh:100:*")).thenReturn(Set.of("refresh:100:abc"));

        service.changePassword(100L, new ChangePasswordDTO("oldpass", "newpass123"));

        verify(userAuthManager).updateById(any(UserAuthPO.class));
        verify(redisTemplate).delete(Set.of("refresh:100:abc"));
    }

    @Test
    void changePassword_accountNotFound_throws() {
        when(userAuthManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.changePassword(999L, new ChangePasswordDTO("old", "newpass123")));
    }

    @Test
    void changePassword_oldPwdInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setId(100L);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(userAuthManager.getById(100L)).thenReturn(po);
        assertThrows(BizException.class,
            () -> service.changePassword(100L, new ChangePasswordDTO("wrongpass", "newpass123")));
    }

    @Test
    void register_success_returnsTokens() {
        when(userAuthManager.count(any())).thenReturn(0L);
        when(userAuthManager.save(any(UserAuthPO.class))).thenAnswer(invocation -> {
            UserAuthPO po = invocation.getArgument(0);
            po.setId(200L);
            return true;
        });
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = service.register(new RegisterDTO("shopowner", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
        verify(userAuthManager).save(any(UserAuthPO.class));
    }

    @Test
    void register_duplicate_throws() {
        when(userAuthManager.count(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class,
            () -> service.register(new RegisterDTO("shopowner", "pass123")));
        assertEquals(AuthCodeEnum.MERCHANT_EXISTS.getCode(), ex.getCode());
    }
}
