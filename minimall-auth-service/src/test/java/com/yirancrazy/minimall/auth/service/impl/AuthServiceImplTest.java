package com.yirancrazy.minimall.auth.service.impl;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceImplTest {

    @Mock private UserAuthManager userAuthManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthServiceImpl(userAuthManager, jwtUtil, redisTemplate, 900L, 604800L);
    }

    @Test
    void register_success() {
        when(userAuthManager.getOne(any())).thenReturn(null);
        when(userAuthManager.save(any(UserAuthPO.class))).thenAnswer(invocation -> {
            UserAuthPO po = invocation.getArgument(0);
            po.setId(42L);
            return true;
        });
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = authService.register(new RegisterDTO("testuser", "pass123"));
        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
    }

    @Test
    void register_userExists_throws() {
        when(userAuthManager.getOne(any())).thenReturn(new UserAuthPO());
        assertThrows(BizException.class, () -> authService.register(new RegisterDTO("exists", "pass")));
    }

    @Test
    void login_userNotFound_throws() {
        when(userAuthManager.getOne(any())).thenReturn(null);
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
        authService.signOut(1L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:1:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any());
    }
}