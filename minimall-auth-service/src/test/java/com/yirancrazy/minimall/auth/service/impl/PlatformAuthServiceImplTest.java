package com.yirancrazy.minimall.auth.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.AdminCreateDTO;
import com.yirancrazy.minimall.auth.dto.AdminPageDTO;
import com.yirancrazy.minimall.auth.dto.AdminRoleDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.entity.AuthRolePO;
import com.yirancrazy.minimall.auth.entity.AuthUserPO;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.AdminVO;
import com.yirancrazy.minimall.auth.vo.UserInfoVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformAuthServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformAuthServiceImplTest {

    @Mock private AuthUserManager authUserManager;
    @Mock private AuthRoleManager authRoleManager;
    @Mock private AuthTokenBlacklistManager tokenBlacklistManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private PlatformAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().doAnswer(inv -> {
            AuthUserPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(authUserManager).save(any(AuthUserPO.class));
        lenient().when(authUserManager.updateById(any(AuthUserPO.class))).thenReturn(true);
        service = new PlatformAuthServiceImpl(
            authUserManager, authRoleManager, tokenBlacklistManager,
            jwtUtil, redisTemplate, 900L, 604800L);
    }

    private AuthRolePO stubPlatformRole() {
        AuthRolePO role = new AuthRolePO();
        role.setId(3L);
        role.setRoleCode("PLATFORM");
        role.setRoleName("平台管理员");
        when(authRoleManager.getById(3L)).thenReturn(role);
        return role;
    }

    @Test
    void login_success_returnsTokens() {
        stubPlatformRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(300L);
        po.setAccount("admin");
        po.setAccountType(3);
        po.setRoleId(3L);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(jwtUtil.sign(anyLong(), anyString(), anyString(), anyLong(), anyString())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVO vo = service.login(new LoginDTO("admin", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil).sign(eq(300L), eq("admin"), eq("PLATFORM"), eq(3L), anyString());
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
        po.setAccountType(3);
        po.setStatus(2);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("admin", "pass")));
    }

    @Test
    void login_passwordInvalid_throws() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPO po = new AuthUserPO();
        po.setId(300L);
        po.setAccountType(3);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDTO("admin", "wrongpass")));
    }

    @Test
    void signOut_deletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:300:*")).thenReturn(Set.of("refresh:300:abc"));
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        service.signOut(300L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:300:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any());
        verify(tokenBlacklistManager).save(any());
    }


    @Test
    void adminCreate_persists_and_returns_id() {
        when(authUserManager.getOne(any())).thenReturn(null);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setAccount("admin01");
        dto.setPassword("pass1234");
        dto.setNickname("管理员01");

        Long id = service.adminCreate(dto);

        assertNotNull(id);
        verify(authUserManager).save(any(AuthUserPO.class));
    }


    @Test
    void adminCreate_throws_on_duplicate_account() {
        AuthUserPO existing = new AuthUserPO();
        existing.setId(1L);
        existing.setAccount("admin01");
        existing.setAccountType(3);
        when(authUserManager.getOne(any())).thenReturn(existing);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setAccount("admin01");
        dto.setPassword("pass1234");

        assertThrows(BizException.class, () -> service.adminCreate(dto));
        verify(authUserManager, never()).save(any(AuthUserPO.class));
    }


    @Test
    void adminCreate_sets_platform_type_and_encrypts_password() {
        when(authUserManager.getOne(any())).thenReturn(null);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setAccount("admin02");
        dto.setPassword("secret123");

        service.adminCreate(dto);

        verify(authUserManager).save(any(AuthUserPO.class));
    }


    @Test
    void adminPage_delegates_to_manager() {
        AdminPageDTO dto = new AdminPageDTO();
        dto.setLimit(10);

        List<AuthUserPO> mockRecords = new ArrayList<>();
        when(authUserManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        CursorPageVO<AdminVO> result = service.adminPage(dto);

        assertNotNull(result);
        verify(authUserManager).list(any(Wrapper.class));
    }


    @Test
    void adminUpdate_updates_nickname_only() {
        AuthUserPO po = new AuthUserPO();
        po.setId(1L);
        po.setAccount("admin01");
        po.setNickname("旧昵称");
        po.setAccountType(3);
        when(authUserManager.getById(1L)).thenReturn(po);

        AdminUpdateDTO dto = new AdminUpdateDTO();
        dto.setNickname("新昵称");

        boolean ok = service.adminUpdate(1L, dto);

        assertEquals(true, ok);
        assertEquals("新昵称", po.getNickname());
        verify(authUserManager).updateById(po);
    }


    @Test
    void adminUpdate_throws_when_not_found() {
        when(authUserManager.getById(999L)).thenReturn(null);

        AdminUpdateDTO dto = new AdminUpdateDTO();
        dto.setNickname("新昵称");

        assertThrows(BizException.class, () -> service.adminUpdate(999L, dto));
    }


    @Test
    void adminDelete_succeeds() {
        AuthUserPO po = new AuthUserPO();
        po.setId(2L);
        po.setAccount("admin02");
        po.setAccountType(3);
        when(authUserManager.getById(2L)).thenReturn(po);
        when(authUserManager.removeById(2L)).thenReturn(true);

        service.adminDelete(1L, 2L);

        verify(authUserManager).removeById(2L);
    }


    @Test
    void adminDelete_throws_on_delete_self() {
        assertThrows(BizException.class, () -> service.adminDelete(1L, 1L));
        verify(authUserManager, never()).removeById(anyLong());
    }


    @Test
    void adminDelete_throws_when_not_found() {
        when(authUserManager.getById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.adminDelete(1L, 999L));
    }


    @Test
    void me_validToken_returnsUserInfo() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("9000000000000000001");
        when(claims.get("account", String.class)).thenReturn("admin");
        when(claims.get("role", String.class)).thenReturn("PLATFORM");
        when(claims.get("roleId", Long.class)).thenReturn(3L);
        when(jwtUtil.parse("access-token")).thenReturn(claims);

        UserInfoVO vo = service.me("access-token");

        assertEquals(9000000000000000001L, vo.getUserId());
        assertEquals("admin", vo.getAccount());
        assertEquals("PLATFORM", vo.getRole());
        assertEquals(3L, vo.getRoleId());
    }


    @Test
    void me_invalidToken_throws() {
        when(jwtUtil.parse("bad-token")).thenThrow(new JwtException("expired"));

        BizException ex = assertThrows(BizException.class, () -> service.me("bad-token"));
        assertEquals(AuthCodeEnum.TOKEN_INVALID.getCode(), ex.getCode());
    }


    @Test
    void adminCreate_sets_contact_and_resolves_role() {
        when(authUserManager.getOne(any())).thenReturn(null);
        AuthRolePO role = new AuthRolePO();
        role.setId(7L);
        role.setRoleCode("OPERATOR");
        when(authRoleManager.getOne(any())).thenReturn(role);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setAccount("op1");
        dto.setPassword("pass1234");
        dto.setContact("13800000000");
        dto.setRoleCode("OPERATOR");

        service.adminCreate(dto);

        ArgumentCaptor<AuthUserPO> captor = ArgumentCaptor.forClass(AuthUserPO.class);
        verify(authUserManager).save(captor.capture());
        assertEquals("13800000000", captor.getValue().getContact());
        assertEquals(7L, captor.getValue().getRoleId());
    }


    @Test
    void adminAssignRole_success_updates_roleId() {
        AuthUserPO po = new AuthUserPO();
        po.setId(5L);
        po.setAccount("admin05");
        po.setAccountType(3);
        when(authUserManager.getById(5L)).thenReturn(po);
        AuthRolePO role = new AuthRolePO();
        role.setId(9L);
        role.setRoleCode("OPERATOR");
        when(authRoleManager.getOne(any())).thenReturn(role);

        service.adminAssignRole(5L, adminRole("OPERATOR"));

        assertEquals(9L, po.getRoleId());
        verify(authUserManager).updateById(po);
    }


    @Test
    void adminAssignRole_roleNotFound_throws() {
        AuthUserPO po = new AuthUserPO();
        po.setId(5L);
        when(authUserManager.getById(5L)).thenReturn(po);
        when(authRoleManager.getOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service.adminAssignRole(5L, adminRole("NOPE")));
    }


    @Test
    void changePassword_success_updates_and_invalidates_refresh() {
        String salt = "testsalt";
        AuthUserPO po = new AuthUserPO();
        po.setId(5L);
        po.setSalt(salt);
        po.setPasswordHash(BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt()));
        when(authUserManager.getById(5L)).thenReturn(po);
        when(redisTemplate.keys("refresh:5:*")).thenReturn(Set.of("refresh:5:abc"));

        service.changePassword(5L, new ChangePasswordDTO("oldpass", "newpass123"));

        verify(authUserManager).updateById(po);
        verify(redisTemplate).delete(Set.of("refresh:5:abc"));
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
        AuthUserPO po = new AuthUserPO();
        po.setId(5L);
        po.setSalt(salt);
        po.setPasswordHash(BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt()));
        when(authUserManager.getById(5L)).thenReturn(po);

        assertThrows(BizException.class,
            () -> service.changePassword(5L, new ChangePasswordDTO("wrongpass", "newpass123")));
    }

    private AdminRoleDTO adminRole(String roleCode) {
        AdminRoleDTO dto = new AdminRoleDTO();
        dto.setRoleCode(roleCode);
        return dto;
    }
}
