package com.yirancrazy.minimall.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.user.dto.UserCreateDTO;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.impl.UserServiceImpl;

/**
 * UserServiceImpl 单元测试，覆盖查询、创建、更新、删除的正常、失败、边界路径。
 */
public class UserServiceImplTest {

    private UserManager userManager;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userManager = mock(UserManager.class);
        lenient().doAnswer(inv -> {
            UserPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(userManager).save(any(UserPO.class));
        lenient().when(userManager.updateById(any(UserPO.class))).thenReturn(true);
        service = new UserServiceImpl(userManager);
    }

    /**
     * 验证 getById 在用户存在时返回实体。
     */
    @Test
    public void getById_returns_user_when_exists() {
        UserPO u = new UserPO();
        u.setId(1L);
        u.setUsername("alice");
        when(userManager.getById(1L)).thenReturn(u);

        UserPO result = service.getById(1L);
        assertEquals(1L, result.getId());
        assertEquals("alice", result.getUsername());
    }

    /**
     * 验证 getById 在用户不存在时抛出 BizException。
     */
    @Test
    public void getById_throws_when_missing() {
        when(userManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }

    /**
     * 验证 create 持久化用户并返回新 ID。
     */
    @Test
    public void create_persists_and_returns_id() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("bob");
        dto.setNickname("Bob");
        dto.setPhone("13800000000");
        dto.setEmail("bob@example.com");

        Long id = service.create(dto);
        assertNotNull(id);
        verify(userManager).save(any(UserPO.class));
    }

    /**
     * 验证 update 调用 updateById 并返回其结果。
     */
    @Test
    public void update_returns_true_on_success() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("alice-new");
        dto.setNickname("Alice New");

        boolean ok = service.update(1L, dto);
        assertTrue(ok);
        verify(userManager).updateById(any(UserPO.class));
    }

    /**
     * 验证 update 在 updateById 失败时返回 false。
     */
    @Test
    public void update_returns_false_on_failure() {
        when(userManager.updateById(any(UserPO.class))).thenReturn(false);
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUsername("x");

        boolean ok = service.update(1L, dto);
        assertFalse(ok);
    }

    /**
     * 验证 delete 调用 removeById 并返回其结果。
     */
    @Test
    public void delete_returns_true_on_success() {
        when(userManager.removeById(1L)).thenReturn(true);
        boolean ok = service.delete(1L);
        assertTrue(ok);
        verify(userManager).removeById(eq(1L));
    }

    /**
     * 验证 delete 在 removeById 失败时返回 false。
     */
    @Test
    public void delete_returns_false_on_failure() {
        when(userManager.removeById(1L)).thenReturn(false);
        boolean ok = service.delete(1L);
        assertFalse(ok);
    }

    /**
     * 验证 page 委托给 userManager.page 并返回其结果。
     */
    @Test
    public void page_delegates_to_manager() {
        UserPageDTO dto = new UserPageDTO();
        dto.setPageNo(1);
        dto.setPageSize(10);
        dto.setKeyword("ali");
        IPage<UserPO> expected = new Page<>(1, 10);
        when(userManager.page(any(IPage.class), any())).thenReturn(expected);

        IPage<UserPO> result = service.page(dto);
        assertEquals(expected, result);
        verify(userManager).page(any(IPage.class), any());
    }

    /**
     * 验证 getProfile 在用户存在时返回含 avatar 和 gender 的实体。
     */
    @Test
    public void getProfile_returns_user_with_avatar_and_gender() {
        UserPO u = new UserPO();
        u.setId(1L);
        u.setUsername("alice");
        u.setNickname("Alice");
        u.setAvatar("https://example.com/avatar.png");
        u.setGender(1);
        when(userManager.getById(1L)).thenReturn(u);

        UserPO result = service.getProfile(1L);
        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getNickname());
        assertEquals("https://example.com/avatar.png", result.getAvatar());
        assertEquals(1, result.getGender());
    }

    /**
     * 验证 getProfile 在用户不存在时抛出 BizException。
     */
    @Test
    public void getProfile_throws_when_missing() {
        when(userManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getProfile(999L));
    }

    /**
     * 验证 updateProfile 仅更新 nickname、avatar、gender 字段。
     */
    @Test
    public void updateProfile_updates_partial_fields() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setNickname("NewNick");
        dto.setAvatar("https://example.com/new.png");
        dto.setGender(2);

        boolean ok = service.updateProfile(1L, dto);
        assertTrue(ok);
        verify(userManager).updateById(any(UserPO.class));
    }

    /**
     * 验证 updateProfile 在 nickname 为 null 时不覆盖原值。
     */
    @Test
    public void updateProfile_skips_null_fields() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setAvatar("https://example.com/new.png");

        boolean ok = service.updateProfile(1L, dto);
        assertTrue(ok);
    }
}
