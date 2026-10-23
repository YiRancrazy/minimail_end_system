package com.yirancrazy.minimall.user.service;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDTO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.dto.UserPageDTO;
import com.yirancrazy.minimall.user.dto.UserProfileDTO;
import com.yirancrazy.minimall.user.dto.UserUpdateDTO;
import com.yirancrazy.minimall.user.entity.UserPO;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.impl.UserServiceImpl;
import com.yirancrazy.minimall.user.vo.UserVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserServiceImplTest 单元测试。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public class UserServiceImplTest {

    private UserManager userManager;
    private MinioUtil minioUtil;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userManager = mock(UserManager.class);
        minioUtil = mock(MinioUtil.class);
        lenient().doAnswer(inv -> {
            UserPO p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(System.nanoTime());
            }
            return true;
        }).when(userManager).save(any(UserPO.class));
        lenient().when(userManager.updateById(any(UserPO.class))).thenReturn(true);
        service = new UserServiceImpl(userManager, minioUtil);
    }

    @Test
    public void provision_creates_profile_with_explicit_id_and_random_nickname() {
        when(userManager.getById(9001L)).thenReturn(null);

        service.provision(9001L);

        ArgumentCaptor<UserPO> captor = ArgumentCaptor.forClass(UserPO.class);
        verify(userManager).save(captor.capture());
        UserPO saved = captor.getValue();
        assertEquals(9001L, saved.getId());
        assertTrue(saved.getNickname().matches("薄荷用户\\d{6}"));
        assertEquals(0, saved.getGender());
        assertNull(saved.getAvatar());
    }

    @Test
    public void provision_existing_profile_is_idempotent() {
        UserPO existing = new UserPO();
        existing.setId(9001L);
        existing.setNickname("薄荷用户123456");
        when(userManager.getById(9001L)).thenReturn(existing);

        service.provision(9001L);

        verify(userManager, never()).save(any(UserPO.class));
        verify(userManager, never()).updateById(any(UserPO.class));
    }


    /**
     * 获取相关数据。
     */
    public void getById_returns_user_when_exists() {
        UserPO u = new UserPO();
        u.setId(1L);
        u.setNickname("Alice");
        when(userManager.getById(1L)).thenReturn(u);

        UserPO result = service.getById(1L);
        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getNickname());
    }


    /**
     * 获取相关数据。
     */
    public void getById_throws_when_missing() {
        when(userManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getById(999L));
    }


    /**
     * 获取相关数据。
     */
    public void getSnapshot_returns_user_snapshot() {
        UserPO user = new UserPO();
        user.setId(1L);
        user.setNickname("Alice");
        when(userManager.getById(1L)).thenReturn(user);

        UserSnapshotDTO result = service.getSnapshot(1L);

        assertEquals(1L, result.getUserId());
        assertEquals("Alice", result.getNickname());
        assertEquals("USER", result.getRole());
    }


    /**
     * 更新业务数据。
     */
    public void update_returns_true_on_success() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("Alice New");

        boolean ok = service.update(1L, dto);
        assertTrue(ok);
        verify(userManager).updateById(any(UserPO.class));
    }


    /**
     * 更新业务数据。
     */
    public void update_sets_phoneMasked() {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("Alice New");
        dto.setPhone("13812345678");

        service.update(1L, dto);

        ArgumentCaptor<UserPO> captor = ArgumentCaptor.forClass(UserPO.class);
        verify(userManager).updateById(captor.capture());
        assertEquals("138****5678", captor.getValue().getPhoneMasked());
    }


    /**
     * 更新业务数据。
     */
    public void update_returns_false_on_failure() {
        when(userManager.updateById(any(UserPO.class))).thenReturn(false);
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setNickname("X");

        boolean ok = service.update(1L, dto);
        assertFalse(ok);
    }


    /**
     * 删除业务数据。
     */
    public void delete_returns_true_on_success() {
        when(userManager.removeById(1L)).thenReturn(true);
        boolean ok = service.delete(1L);
        assertTrue(ok);
        verify(userManager).removeById(eq(1L));
    }


    /**
     * 删除业务数据。
     */
    public void delete_returns_false_on_failure() {
        when(userManager.removeById(1L)).thenReturn(false);
        boolean ok = service.delete(1L);
        assertFalse(ok);
    }


    /**
     * 分页查询数据。
     */
    public void page_delegates_to_manager() {
        UserPageDTO dto = new UserPageDTO();
        dto.setLimit(10);
        dto.setKeyword("ali");
        List<UserPO> records = Collections.emptyList();
        when(userManager.list(any(Wrapper.class))).thenReturn(records);

        CursorPageVO<UserPO> result = service.page(dto);
        assertNotNull(result);
        verify(userManager).list(any(Wrapper.class));
    }


    /**
     * 获取相关数据。
     */
    public void getProfile_returns_user_with_avatar_and_gender() {
        UserPO u = new UserPO();
        u.setId(1L);
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
     * 获取相关数据。
     */
    public void getProfile_throws_when_missing() {
        when(userManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getProfile(999L));
    }


    /**
     * 获取相关数据。
     */
    public void getVO_returns_masked_user_vo() {
        UserPO u = new UserPO();
        u.setId(1L);
        u.setPhone("13800000000");
        u.setAvatar("avatar/alice.png");
        when(userManager.getById(1L)).thenReturn(u);
        when(minioUtil.resolvePublicUrl("avatar/alice.png")).thenReturn("https://cdn/avatar/alice.png");

        UserVO vo = service.getVO(1L);

        assertEquals("138****0000", vo.getPhone());
        assertEquals("https://cdn/avatar/alice.png", vo.getAvatar());
    }


    /**
     * 获取相关数据。
     */
    public void getVO_throws_when_missing() {
        when(userManager.getById(999L)).thenReturn(null);
        assertThrows(BizException.class, () -> service.getVO(999L));
    }


    /**
     * 获取相关数据。
     */
    public void getProfileVO_returns_vo() {
        UserPO u = new UserPO();
        u.setId(1L);
        u.setNickname("Alice");
        when(userManager.getById(1L)).thenReturn(u);

        UserVO vo = service.getProfileVO(1L);

        assertEquals(1L, vo.getId());
        assertEquals("Alice", vo.getNickname());
    }


    /**
     * 更新业务数据。
     */
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
     * 更新业务数据。
     */
    public void updateProfile_skips_null_fields() {
        UserProfileDTO dto = new UserProfileDTO();
        dto.setAvatar("https://example.com/new.png");

        boolean ok = service.updateProfile(1L, dto);
        assertTrue(ok);
    }
}
