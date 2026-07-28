package com.yirancrazy.minimall.auth.service.impl;

import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.dto.RegisterDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private UserAuthManager manager;
    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        manager = mock(UserAuthManager.class);
        JwtUtil jwtUtil = new JwtUtil("test-secret-1234567890123456789012", 3600L);
        service = new AuthServiceImpl(manager, jwtUtil, 3600L);
        ReflectionTestUtils.setField(service, "ttlSeconds", 3600L);
    }

    @Test
    void register_then_login_returns_token_and_validates_bcrypt() {
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(null);
        when(manager.save(ArgumentMatchers.any(UserAuthPO.class)))
            .thenAnswer(inv -> {
                UserAuthPO p = inv.getArgument(0);
                p.setId(42L);
                return true;
            });

        TokenVO tok = service.register(new RegisterDTO("alice", "secret"));
        assertNotNull(tok.getAccessToken());
        assertEquals("Bearer", tok.getTokenType());

        UserAuthPO stored = new UserAuthPO();
        stored.setId(42L);
        stored.setUsername("alice");
        stored.setPasswordHash(BCrypt.hashpw("secret" + "", BCrypt.gensalt()));
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(stored);

        TokenVO tok2 = service.login(new LoginDTO("alice", "secret"));
        assertNotNull(tok2.getAccessToken());
    }

    @Test
    void login_unknown_user_throws_biz_exception() {
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.login(new LoginDTO("ghost", "x")));
    }

    @Test
    void me_parses_token_roundtrip() {
        String token = service.register(new RegisterDTO() {{ setUsername("bob"); setPassword("pwd"); }}).getAccessToken();
        UserInfoVO info = service.me(token);
        assertEquals("bob", info.getUsername());
        assertEquals("USER", info.getRole());
    }
}