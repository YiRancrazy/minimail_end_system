package com.yirancrazy.minimall.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.auth.TokenVo;
import com.yirancrazy.minimall.auth.constant.AuthCodeEnum;
import com.yirancrazy.minimall.auth.dto.AdminCreateDto;
import com.yirancrazy.minimall.auth.dto.AdminPageDto;
import com.yirancrazy.minimall.auth.dto.AdminRoleDto;
import com.yirancrazy.minimall.auth.dto.ChangePasswordDto;
import com.yirancrazy.minimall.auth.dto.LoginDto;
import com.yirancrazy.minimall.auth.entity.AuthRolePo;
import com.yirancrazy.minimall.auth.entity.AuthUserPo;
import com.yirancrazy.minimall.auth.manager.AuthRoleManager;
import com.yirancrazy.minimall.auth.manager.AuthTokenBlacklistManager;
import com.yirancrazy.minimall.auth.manager.AuthUserManager;
import com.yirancrazy.minimall.auth.util.JwtUtil;
import com.yirancrazy.minimall.auth.vo.AdminVo;
import com.yirancrazy.minimall.auth.vo.UserInfoVo;
import com.yirancrazy.minimall.common.constant.jwt.JwtType;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台管理员认证与管理服务单元测试；覆盖登录、会话、创建、分页、角色、删除和密码流程。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
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
        lenient()
                .doAnswer(
                        inv -> {
                            AuthUserPo p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId(System.nanoTime());
                            }
                            return true;
                        })
                .when(authUserManager)
                .save(any(AuthUserPo.class));
        lenient().when(authUserManager.updateById(any(AuthUserPo.class))).thenReturn(true);
        service =
                new PlatformAuthServiceImpl(
                        authUserManager,
                        authRoleManager,
                        tokenBlacklistManager,
                        jwtUtil,
                        redisTemplate,
                        900_000L,
                        604_800_000L);
    }

    private AuthRolePo stubPlatformRole() {
        AuthRolePo role = new AuthRolePo();
        role.setId(3L);
        role.setRoleCode("PLATFORM");
        role.setRoleName("平台管理员");
        when(authRoleManager.getById(3L)).thenReturn(role);
        return role;
    }

    /** 验证管理员登录成功：账号类型为 3、启用且密码匹配，登录后预期返回 PLATFORM 角色双令牌. */
    @Test
    void testLoginSuccessReturnsTokens() {
        stubPlatformRole();
        String salt = "testsalt";
        String hash = BCrypt.hashpw("pass123" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(300L);
        po.setAccount("admin");
        po.setAccountType(3);
        po.setRoleId(3L);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        when(jwtUtil.sign(
                        anyLong(),
                        anyLong(),
                        anyString(),
                        anyString(),
                        anyLong(),
                        anyString(),
                        eq(JwtType.ACCESS),
                        eq(900_000L)))
                .thenReturn("access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-token");

        TokenVo vo = service.login(new LoginDto("admin", "pass123"));

        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        verify(jwtUtil)
                .sign(
                        eq(300L),
                        eq(300L),
                        eq("admin"),
                        eq("PLATFORM"),
                        eq(3L),
                        anyString(),
                        eq(JwtType.ACCESS),
                        eq(900_000L));
    }

    /** 验证管理员账号不存在时登录失败：前置账号查询为空，调用登录后预期抛出 BizException. */
    @Test
    void testLoginAccountNotFoundThrows() {
        when(authUserManager.getOne(any())).thenReturn(null);
        assertThrows(BizException.class, () -> service.login(new LoginDto("nobody", "pass")));
    }

    /** 验证非平台账号登录管理员入口被拒绝：前置类型为 1，调用登录后预期抛出角色不匹配异常. */
    @Test
    void testLoginRoleMismatchThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(1);
        po.setStatus(1);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDto("alice", "pass")));
    }

    /** 验证禁用平台账号不能登录：前置类型为 3 且状态为 2，调用登录后预期抛出账号禁用异常. */
    @Test
    void testLoginAccountDisabledThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setAccountType(3);
        po.setStatus(2);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDto("admin", "pass")));
    }

    /** 验证管理员错误密码被拒绝：前置账号启用但密码不匹配，调用登录后预期返回密码错误异常. */
    @Test
    void testLoginPasswordInvalidThrows() {
        String salt = "testsalt";
        String hash = BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt());
        AuthUserPo po = new AuthUserPo();
        po.setId(300L);
        po.setAccountType(3);
        po.setStatus(1);
        po.setSalt(salt);
        po.setPasswordHash(hash);
        when(authUserManager.getOne(any())).thenReturn(po);
        assertThrows(BizException.class, () -> service.login(new LoginDto("admin", "wrongpass")));
    }

    /** 验证管理员退出清理会话：前置存在刷新令牌，退出后预期删除刷新键并写入两类黑名单记录. */
    @Test
    void testSignOutDeletesRefreshAndBlacklistsJti() {
        when(redisTemplate.keys("refresh:300:*")).thenReturn(Set.of("refresh:300:abc"));
        when(tokenBlacklistManager.save(any())).thenReturn(true);
        service.signOut(300L, "abc");
        verify(redisTemplate).delete(Set.of("refresh:300:abc"));
        verify(valueOperations).set(eq("blacklist:jti:abc"), eq("1"), eq(900L), any());
        verify(tokenBlacklistManager).save(any());
    }

    /** 验证管理员创建返回 ID：前置账号未占用且保存生成主键，执行后预期返回非空账号 ID. */
    @Test
    void testAdminCreatePersistsAndReturnsId() {
        when(authUserManager.getOne(any())).thenReturn(null);

        AdminCreateDto dto = new AdminCreateDto();
        dto.setAccount("admin01");
        dto.setPassword("pass1234");

        Long id = service.adminCreate(dto);

        assertNotNull(id);
        verify(authUserManager).save(any(AuthUserPo.class));
    }

    /** 验证重复管理员账号被拒绝：前置账号已存在，创建后预期抛错且不保存新账号. */
    @Test
    void testAdminCreateThrowsOnDuplicateAccount() {
        AuthUserPo existing = new AuthUserPo();
        existing.setId(1L);
        existing.setAccount("admin01");
        existing.setAccountType(3);
        when(authUserManager.getOne(any())).thenReturn(existing);

        AdminCreateDto dto = new AdminCreateDto();
        dto.setAccount("admin01");
        dto.setPassword("pass1234");

        assertThrows(BizException.class, () -> service.adminCreate(dto));
        verify(authUserManager, never()).save(any(AuthUserPo.class));
    }

    /** 验证管理员进入保存路径：前置账号不存在且入参合法，创建后预期调用保存；用例未断言散列字段. */
    @Test
    void testAdminCreateSetsPlatformTypeAndEncryptsPassword() {
        when(authUserManager.getOne(any())).thenReturn(null);

        AdminCreateDto dto = new AdminCreateDto();
        dto.setAccount("admin02");
        dto.setPassword("secret123");

        service.adminCreate(dto);

        verify(authUserManager).save(any(AuthUserPo.class));
    }

    /** 验证管理员分页调用管理器：前置 list 返回空记录，使用 limit 10 查询后预期返回非空分页对象. */
    @Test
    void testAdminPageDelegatesToManager() {
        AdminPageDto dto = new AdminPageDto();
        dto.setLimit(10);

        List<AuthUserPo> mockRecords = new ArrayList<>();
        when(authUserManager.list(any(Wrapper.class))).thenReturn(mockRecords);

        CursorPageVo<AdminVo> result = service.adminPage(dto);

        assertNotNull(result);
        verify(authUserManager).list(any(Wrapper.class));
    }

    /** 验证管理员删除成功：前置操作者与目标不同且目标存在，删除后预期按目标 ID 移除账号. */
    @Test
    void testAdminDeleteSucceeds() {
        AuthUserPo po = new AuthUserPo();
        po.setId(2L);
        po.setAccount("admin02");
        po.setAccountType(3);
        when(authUserManager.getById(2L)).thenReturn(po);
        when(authUserManager.removeById(2L)).thenReturn(true);

        service.adminDelete(1L, 2L);

        verify(authUserManager).removeById(2L);
    }

    /** 验证管理员不能删除自身：前置操作者与目标 ID 相同，调用后预期抛错且不执行删除. */
    @Test
    void testAdminDeleteThrowsOnDeleteSelf() {
        assertThrows(BizException.class, () -> service.adminDelete(1L, 1L));
        verify(authUserManager, never()).removeById(anyLong());
    }

    /** 验证不存在管理员不能删除：前置目标查询为空，调用删除后预期抛出 BizException. */
    @Test
    void testAdminDeleteThrowsWhenNotFound() {
        when(authUserManager.getById(999L)).thenReturn(null);

        assertThrows(BizException.class, () -> service.adminDelete(1L, 999L));
    }

    /** 验证有效 JWT 查询管理员信息：前置声明含账号和 PLATFORM 角色，调用后预期账号 ID 兼作业务用户 ID. */
    @Test
    void testMeValidTokenReturnsUserInfo() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("9000000000000000001");
        when(claims.get("account", String.class)).thenReturn("admin");
        when(claims.get("role", String.class)).thenReturn("PLATFORM");
        when(claims.get("roleId", Long.class)).thenReturn(3L);
        when(jwtUtil.parse("access-token")).thenReturn(claims);

        UserInfoVo vo = service.me("access-token");

        assertEquals(9000000000000000001L, vo.getUserId());
        assertEquals("admin", vo.getAccount());
        assertEquals("PLATFORM", vo.getRole());
        assertEquals(3L, vo.getRoleId());
    }

    /** 验证无效 JWT 查询管理员信息失败：前置解析抛异常，调用 me 后预期转换为令牌无效错误. */
    @Test
    void testMeInvalidTokenThrows() {
        when(jwtUtil.parse("bad-token")).thenThrow(new JwtException("expired"));

        BizException ex = assertThrows(BizException.class, () -> service.me("bad-token"));
        assertEquals(AuthCodeEnum.TOKEN_INVALID.getCode(), ex.getCode());
    }

    /** 验证管理员保存联系方式和角色：前置 OPERATOR 映射角色 7，创建后预期联系方式和角色 ID 正确. */
    @Test
    void testAdminCreateSetsContactAndResolvesRole() {
        when(authUserManager.getOne(any())).thenReturn(null);
        AuthRolePo role = new AuthRolePo();
        role.setId(7L);
        role.setRoleCode("OPERATOR");
        when(authRoleManager.getOne(any())).thenReturn(role);

        AdminCreateDto dto = new AdminCreateDto();
        dto.setAccount("op1");
        dto.setPassword("pass1234");
        dto.setContact("13800000000");
        dto.setRoleCode("OPERATOR");

        service.adminCreate(dto);

        ArgumentCaptor<AuthUserPo> captor = ArgumentCaptor.forClass(AuthUserPo.class);
        verify(authUserManager).save(captor.capture());
        assertEquals("13800000000", captor.getValue().getContact());
        assertEquals(7L, captor.getValue().getRoleId());
    }

    /** 验证管理员角色分配成功：前置 OPERATOR 映射角色 9，分配后预期账号角色更新并执行持久化. */
    @Test
    void testAdminAssignRoleSuccessUpdatesRoleId() {
        AuthUserPo po = new AuthUserPo();
        po.setId(5L);
        po.setAccount("admin05");
        po.setAccountType(3);
        when(authUserManager.getById(5L)).thenReturn(po);
        AuthRolePo role = new AuthRolePo();
        role.setId(9L);
        role.setRoleCode("OPERATOR");
        when(authRoleManager.getOne(any())).thenReturn(role);

        service.adminAssignRole(5L, adminRole("OPERATOR"));

        assertEquals(9L, po.getRoleId());
        verify(authUserManager).updateById(po);
    }

    /** 验证未知角色分配失败：前置目标账号存在但角色查询为空，调用分配后预期抛错且不更新账号. */
    @Test
    void testAdminAssignRoleRoleNotFoundThrows() {
        AuthUserPo po = new AuthUserPo();
        po.setId(5L);
        when(authUserManager.getById(5L)).thenReturn(po);
        when(authRoleManager.getOne(any())).thenReturn(null);

        assertThrows(BizException.class, () -> service.adminAssignRole(5L, adminRole("NOPE")));
    }

    /** 验证管理员修改密码成功：前置旧密码正确，修改后预期更新密码散列并删除全部刷新令牌. */
    @Test
    void testChangePasswordSuccessUpdatesAndInvalidatesRefresh() {
        String salt = "testsalt";
        AuthUserPo po = new AuthUserPo();
        po.setId(5L);
        po.setSalt(salt);
        po.setPasswordHash(BCrypt.hashpw("oldpass" + salt, BCrypt.gensalt()));
        when(authUserManager.getById(5L)).thenReturn(po);
        when(redisTemplate.keys("refresh:5:*")).thenReturn(Set.of("refresh:5:abc"));

        service.changePassword(5L, new ChangePasswordDto("oldpass", "newpass123"));

        verify(authUserManager).updateById(po);
        verify(redisTemplate).delete(Set.of("refresh:5:abc"));
    }

    /** 验证管理员修改密码账号不存在：前置账号查询为空，调用修改后预期抛出 BizException. */
    @Test
    void testChangePasswordAccountNotFoundThrows() {
        when(authUserManager.getById(999L)).thenReturn(null);
        assertThrows(
                BizException.class,
                () -> service.changePassword(999L, new ChangePasswordDto("old", "newpass123")));
    }

    /** 验证管理员旧密码错误拒绝修改：前置密码散列不匹配，调用修改后预期抛出 BizException. */
    @Test
    void testChangePasswordOldPwdInvalidThrows() {
        String salt = "testsalt";
        AuthUserPo po = new AuthUserPo();
        po.setId(5L);
        po.setSalt(salt);
        po.setPasswordHash(BCrypt.hashpw("correctpass" + salt, BCrypt.gensalt()));
        when(authUserManager.getById(5L)).thenReturn(po);

        assertThrows(
                BizException.class,
                () -> service.changePassword(5L, new ChangePasswordDto("wrongpass", "newpass123")));
    }

    private AdminRoleDto adminRole(String roleCode) {
        AdminRoleDto dto = new AdminRoleDto();
        dto.setRoleCode(roleCode);
        return dto;
    }
}
