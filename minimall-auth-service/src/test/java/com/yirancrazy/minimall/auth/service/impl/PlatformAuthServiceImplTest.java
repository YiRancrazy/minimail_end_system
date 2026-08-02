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
import org.springframework.security.crypto.bcrypt.BCrypt;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformAuthServiceImpl 的单元测试类，覆盖登录/登出正常与失败场景。
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformAuthServiceImplTest {

    @Mock private UserAuthManager userAuthManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private PlatformAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new PlatformAuthServiceImpl(userAuthManager, jwtUtil, redisTemplate, 900L, 604800L);
    }

    @Test
    void login_success_returnsTokens() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setId(300L);
        po.setUsername("admin");
        po.setRole("PLATFORM");
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(userAuthManager.getOne(any())).thenReturn(po);
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = service.login(new LoginDTO("admin", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil).sign(eq(300L), eq("admin"), eq("PLATFORM"), anyString());
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
        po.setRole("PLATFORM");
        po.setStatus(2);
        when(userAuthManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("admin", "pass")));
    }

    @Test
    void login_passwordInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        UserAuthPO po = new UserAuthPO();
        po.setId(300L);
        po.setRole("PLATFORM");
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(userAuthManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("admin", "wrongpass")));
    }

    @Test
    void signOut_deletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:300:*")).thenReturn(Set.of("refresh:300:abc"));
        service.signOut(300L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:300:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any());
    }
}
