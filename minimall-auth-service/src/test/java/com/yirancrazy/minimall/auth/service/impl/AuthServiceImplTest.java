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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
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
    void register_returns_token_for_new_user() {
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
    }

    @Test
    void login_unknown_user_throws_biz_exception() {
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(null);
        assertThrows(BizException.class,
            () -> service.login(new LoginDTO("ghost", "x")));
    }

    @Test
    void login_existing_user_returns_token_after_register() {
        // First call: capture the saved UserAuthPO so we have salt+hash.
        ArgumentCaptor<UserAuthPO> cap = ArgumentCaptor.forClass(UserAuthPO.class);
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(null);
        when(manager.save(cap.capture()))
            .thenAnswer(inv -> {
                UserAuthPO p = inv.getArgument(0);
                p.setId(42L);
                return true;
            });
        service.register(new RegisterDTO("alice", "secret"));
        UserAuthPO saved = cap.getValue();
        assertNotNull(saved.getSalt());

        // Reset mock: register flow is irrelevant now, simulate "user already exists".
        org.mockito.Mockito.reset(manager);
        UserAuthPO existing = new UserAuthPO();
        existing.setId(42L);
        existing.setUsername("alice");
        existing.setSalt(saved.getSalt());
        existing.setPasswordHash(saved.getPasswordHash());
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(existing);

        TokenVO tok = service.login(new LoginDTO("alice", "secret"));
        assertNotNull(tok.getAccessToken());
    }

    @Test
    void me_parses_token_roundtrip() {
        when(manager.getOne(ArgumentMatchers.any())).thenReturn(null);
        when(manager.save(ArgumentMatchers.any(UserAuthPO.class)))
            .thenAnswer(inv -> {
                UserAuthPO p = inv.getArgument(0);
                p.setId(99L);
                return true;
            });
        String token = service.register(new RegisterDTO("bob", "pwd")).getAccessToken();
        UserInfoVO info = service.me(token);
        assertEquals("bob", info.getUsername());
        assertEquals("USER", info.getRole());
    }
}