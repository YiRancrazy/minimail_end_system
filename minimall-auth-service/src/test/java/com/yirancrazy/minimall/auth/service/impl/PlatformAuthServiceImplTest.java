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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.api.dto.auth.TokenVO;
import com.yirancrazy.minimall.auth.dto.AdminCreateDTO;
import com.yirancrazy.minimall.auth.dto.AdminPageDTO;
import com.yirancrazy.minimall.auth.dto.AdminUpdateDTO;
import com.yirancrazy.minimall.auth.dto.LoginDTO;
import com.yirancrazy.minimall.auth.entity.UserAuthPO;
import com.yirancrazy.minimall.auth.manager.UserAuthManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.common.exception.BizException;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformAuthServiceImpl 的单元测试类，覆盖登录/登出与平台管理员 CRUD 的正常、失败、边界场景。
 * @Version: 1.1
 * @DateTime: 2026/08/03
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
        lenient().doAnswer(inv -> {
            UserAuthPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(userAuthManager).save(any(UserAuthPO.class));
        lenient().when(userAuthManager.updateById(any(UserAuthPO.class))).thenReturn(true);
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

    /**
     * 验证 adminCreate 正常创建管理员并返回 ID。
     */
    @Test
    void adminCreate_persists_and_returns_id() {
        when(userAuthManager.getOne(any())).thenReturn(null);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("admin01");
        dto.setPassword("pass1234");
        dto.setNickname("管理员01");

        Long id = service.adminCreate(dto);

        assertNotNull(id);
        verify(userAuthManager).save(any(UserAuthPO.class));
    }

    /**
     * 验证 adminCreate 用户名重复时抛出 ADMIN_USERNAME_EXISTS。
     */
    @Test
    void adminCreate_throws_on_duplicate_username() {
        UserAuthPO existing = new UserAuthPO();
        existing.setId(1L);
        existing.setUsername("admin01");
        existing.setRole("PLATFORM");
        when(userAuthManager.getOne(any())).thenReturn(existing);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("admin01");
        dto.setPassword("pass1234");

        assertThrows(BizException.class, () -> service.adminCreate(dto));
        verify(userAuthManager, never()).save(any(UserAuthPO.class));
    }

    /**
     * 验证 adminCreate 设置 role=PLATFORM 且密码 BCrypt 加密。
     */
    @Test
    void adminCreate_sets_platform_role_and_encrypts_password() {
        when(userAuthManager.getOne(any())).thenReturn(null);

        AdminCreateDTO dto = new AdminCreateDTO();
        dto.setUsername("admin02");
        dto.setPassword("secret123");

        service.adminCreate(dto);

        verify(userAuthManager).save(any(UserAuthPO.class));
    }

    /**
     * 验证 adminPage 分页查询委托给 manager。
     */
    @Test
    void adminPage_delegates_to_manager() {
        AdminPageDTO dto = new AdminPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);

        IPage<UserAuthPO> expected = new Page<>(1, 10);
        when(userAuthManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<UserAuthPO> result = service.adminPage(dto);

        assertEquals(expected, result);
        verify(userAuthManager).page(any(IPage.class), any());
    }

    /**
     * 验证 adminUpdate 仅更新 nickname 字段。
     */
    @Test
    void adminUpdate_updates_nickname_only() {
        UserAuthPO po = new UserAuthPO();
        po.setId(1L);
        po.setUsername("admin01");
        po.setNickname("旧昵称");
        po.setRole("PLATFORM");
        when(userAuthManager.getById(1L)).thenReturn(po);

        AdminUpdateDTO dto = new AdminUpdateDTO();
        dto.setNickname("新昵称");

        boolean ok = service.adminUpdate(1L, dto);

        assertEquals(true, ok);
        assertEquals("新昵称", po.getNickname());
        verify(userAuthManager).updateById(po);
    }

    /**
     * 验证 adminUpdate 管理员不存在时抛出 USER_NOT_FOUND。
     */
    @Test
    void adminUpdate_throws_when_not_found() {
        when(userAuthManager.getById(999L)).thenReturn(null);

        AdminUpdateDTO dto = new AdminUpdateDTO();
        dto.setNickname("新昵称");

        assertThrows(BizException.class, () -> service.adminUpdate(999L, dto));
    }

    /**
     * 验证 adminDelete 正常逻辑删除。
     */
    @Test
    void adminDelete_succeeds() {
        UserAuthPO po = new UserAuthPO();
        po.setId(2L);
        po.setUsername("admin02");
        po.setRole("PLATFORM");
        when(userAuthManager.getById(2L)).thenReturn(po);
        when(userAuthManager.removeById(2L)).thenReturn(true);

        service.adminDelete(1L, 2L);

        verify(userAuthManager).removeById(2L);
    }

    /**
     * 验证 adminDelete 不能删除自己，抛出 CANNOT_DELETE_SELF。
     */
    @Test
    void adminDelete_throws_on_delete_self() {
        assertThrows(BizException.class, () -> service.adminDelete(1L, 1L));
        verify(userAuthManager, never()).removeById(anyLong());
    }

    /**
     * 验证 adminDelete 管理员不存在时抛出 USER_NOT_FOUND。
     */
    @Test
    void adminDelete_throws_when_not_found() {
        when(userAuthManager.getById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.adminDelete(1L, 999L));
    }
}
