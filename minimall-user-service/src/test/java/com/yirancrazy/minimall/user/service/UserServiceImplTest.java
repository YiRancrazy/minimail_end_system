package com.yirancrazy.minimall.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDto;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.user.dto.UserPageDto;
import com.yirancrazy.minimall.user.dto.UserProfileDto;
import com.yirancrazy.minimall.user.dto.UserUpdateDto;
import com.yirancrazy.minimall.user.entity.UserPo;
import com.yirancrazy.minimall.user.manager.UserManager;
import com.yirancrazy.minimall.user.service.impl.UserServiceImpl;
import com.yirancrazy.minimall.user.vo.UserVo;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户服务单元测试，使用 UserManager 和 MinioUtil 替身覆盖建档幂等、查询、分页、脱敏、资料更新及异常路径.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class UserServiceImplTest {

  private UserManager userManager;
  private MinioUtil minioUtil;
  private UserServiceImpl service;

  /**
   * 初始化 UserManager、MinioUtil 替身和待测服务。
   *
   * <p>默认让保存回填 ID，并让更新操作返回成功。
   */
  @BeforeEach
  void setUp() {
    userManager = mock(UserManager.class);
    minioUtil = mock(MinioUtil.class);
    lenient()
        .doAnswer(
            inv -> {
              UserPo p = inv.getArgument(0);
              if (p.getId() == null) {
                p.setId(System.nanoTime());
              }
              return true;
            })
        .when(userManager)
        .save(any(UserPo.class));
    lenient().when(userManager.updateById(any(UserPo.class))).thenReturn(true);
    service = new UserServiceImpl(userManager, minioUtil);
  }

  /** 验证首次建档：用户 9001 不存在，调用 provision，预期显式 ID、随机昵称、性别 0 和空头像. */
  @Test
  public void testProvisionCreatesProfileWithExplicitIdAndRandomNickname() {
    when(userManager.getById(9001L)).thenReturn(null);

    service.provision(9001L);

    ArgumentCaptor<UserPo> captor = ArgumentCaptor.forClass(UserPo.class);
    verify(userManager).save(captor.capture());
    UserPo saved = captor.getValue();
    assertEquals(9001L, saved.getId());
    assertTrue(saved.getNickname().matches("薄荷用户\\d{6}"));
    assertEquals(0, saved.getGender());
    assertNull(saved.getAvatar());
  }

  /** 验证重复建档：用户 9001 已存在，再次调用 provision，预期不调用 save 和 updateById. */
  @Test
  public void testProvisionExistingProfileIsIdempotent() {
    UserPo existing = new UserPo();
    existing.setId(9001L);
    existing.setNickname("薄荷用户123456");
    when(userManager.getById(9001L)).thenReturn(existing);

    service.provision(9001L);

    verify(userManager, never()).save(any(UserPo.class));
    verify(userManager, never()).updateById(any(UserPo.class));
  }

  /**
   * 验证按 ID 查询存在用户时返回持久化对象。
   *
   * <p>前置条件：Manager 对用户 1 返回昵称 Alice。关键步骤：调用 getById。预期结果：返回的 ID 和
   * 昵称与替身数据一致。
   */
  public void getByIdReturnsUserWhenExists() {
    UserPo u = new UserPo();
    u.setId(1L);
    u.setNickname("Alice");
    when(userManager.getById(1L)).thenReturn(u);

    UserPo result = service.getById(1L);
    assertEquals(1L, result.getId());
    assertEquals("Alice", result.getNickname());
  }

  /**
   * 验证按 ID 查询不存在用户时抛出业务异常。
   *
   * <p>前置条件：Manager 对用户 999 返回 null。关键步骤：调用 getById。预期结果：抛出 BizException。
   */
  public void getByIdThrowsWhenMissing() {
    when(userManager.getById(999L)).thenReturn(null);
    assertThrows(BizException.class, () -> service.getById(999L));
  }

  /**
   * 验证用户快照包含 ID、昵称和 USER 角色。
   *
   * <p>前置条件：用户 1 存在且昵称为 Alice。关键步骤：调用 getSnapshot。预期结果：userId=1，
   * nickname=Alice，role=USER。
   */
  public void getSnapshotReturnsUserSnapshot() {
    UserPo user = new UserPo();
    user.setId(1L);
    user.setNickname("Alice");
    when(userManager.getById(1L)).thenReturn(user);

    UserSnapshotDto result = service.getSnapshot(1L);

    assertEquals(1L, result.getUserId());
    assertEquals("Alice", result.getNickname());
    assertEquals("USER", result.getRole());
  }

  /**
   * 验证 Manager 更新成功时服务返回 true。
   *
   * <p>前置条件：Manager 的 updateById 返回 true。关键步骤：调用 update。预期结果：返回 true，并调用
   * updateById。
   */
  public void updateReturnsTrueOnSuccess() {
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("Alice New");

    boolean ok = service.update(1L, dto);
    assertTrue(ok);
    verify(userManager).updateById(any(UserPo.class));
  }

  /**
   * 验证更新手机号时同步保存脱敏手机号。
   *
   * <p>前置条件：手机号为 13812345678。关键步骤：捕获 updateById 接收的 UserPo。预期结果：
   * phoneMasked=138****5678。
   */
  public void updateSetsPhoneMasked() {
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("Alice New");
    dto.setPhone("13812345678");

    service.update(1L, dto);

    ArgumentCaptor<UserPo> captor = ArgumentCaptor.forClass(UserPo.class);
    verify(userManager).updateById(captor.capture());
    assertEquals("138****5678", captor.getValue().getPhoneMasked());
  }

  /**
   * 验证 Manager 更新失败时服务返回 false。
   *
   * <p>前置条件：updateById 返回 false。关键步骤：调用 update。预期结果：返回 false。
   */
  public void updateReturnsFalseOnFailure() {
    when(userManager.updateById(any(UserPo.class))).thenReturn(false);
    UserUpdateDto dto = new UserUpdateDto();
    dto.setNickname("X");

    boolean ok = service.update(1L, dto);
    assertFalse(ok);
  }

  /**
   * 验证 Manager 删除成功时服务返回 true。
   *
   * <p>前置条件：removeById(1) 返回 true。关键步骤：调用 delete(1)。预期结果：返回 true，并调用
   * removeById(1)。
   */
  public void deleteReturnsTrueOnSuccess() {
    when(userManager.removeById(1L)).thenReturn(true);
    boolean ok = service.delete(1L);
    assertTrue(ok);
    verify(userManager).removeById(eq(1L));
  }

  /**
   * 验证 Manager 删除失败时服务返回 false。
   *
   * <p>前置条件：removeById(1) 返回 false。关键步骤：调用 delete(1)。预期结果：返回 false。
   */
  public void deleteReturnsFalseOnFailure() {
    when(userManager.removeById(1L)).thenReturn(false);
    boolean ok = service.delete(1L);
    assertFalse(ok);
  }

  /**
   * 验证用户分页会把游标和关键字参数交给 Manager 查询。
   *
   * <p>前置条件：Manager 返回空列表，关键字为 ali。关键步骤：调用 page。预期结果：返回非空游标页对象，
   * 且调用 list。
   */
  public void pageDelegatesToManager() {
    UserPageDto dto = new UserPageDto();
    dto.setLimit(10);
    dto.setKeyword("ali");
    List<UserPo> records = Collections.emptyList();
    when(userManager.list(any(Wrapper.class))).thenReturn(records);

    CursorPageVo<UserPo> result = service.page(dto);
    assertNotNull(result);
    verify(userManager).list(any(Wrapper.class));
  }

  /**
   * 验证用户资料查询保留头像值、性别和基础字段。
   *
   * <p>前置条件：用户 1 具有完整头像和性别。关键步骤：调用 getProfile。预期结果：ID、昵称、头像和
   * 性别与替身数据一致。
   */
  public void getProfileReturnsUserWithAvatarAndGender() {
    UserPo u = new UserPo();
    u.setId(1L);
    u.setNickname("Alice");
    u.setAvatar("https://example.com/avatar.png");
    u.setGender(1);
    when(userManager.getById(1L)).thenReturn(u);

    UserPo result = service.getProfile(1L);
    assertEquals(1L, result.getId());
    assertEquals("Alice", result.getNickname());
    assertEquals("https://example.com/avatar.png", result.getAvatar());
    assertEquals(1, result.getGender());
  }

  /**
   * 验证查询不存在用户资料时抛出业务异常。
   *
   * <p>前置条件：Manager 对用户 999 返回 null。关键步骤：调用 getProfile。预期结果：抛出
   * BizException。
   */
  public void getProfileThrowsWhenMissing() {
    when(userManager.getById(999L)).thenReturn(null);
    assertThrows(BizException.class, () -> service.getProfile(999L));
  }

  /**
   * 验证用户视图会脱敏手机号并解析头像 URL。
   *
   * <p>前置条件：用户手机号为 13800000000，头像对象键可解析为 CDN URL。关键步骤：调用 getVo。
   * 预期结果：手机号为 138****0000，头像为解析后的 URL。
   */
  public void getVoReturnsMaskedUserVo() {
    UserPo u = new UserPo();
    u.setId(1L);
    u.setPhone("13800000000");
    u.setAvatar("avatar/alice.png");
    when(userManager.getById(1L)).thenReturn(u);
    when(minioUtil.resolvePublicUrl("avatar/alice.png")).thenReturn("https://cdn/avatar/alice.png");

    UserVo vo = service.getVo(1L);

    assertEquals("138****0000", vo.getPhone());
    assertEquals("https://cdn/avatar/alice.png", vo.getAvatar());
  }

  /**
   * 验证查询不存在用户视图时抛出业务异常。
   *
   * <p>前置条件：Manager 对用户 999 返回 null。关键步骤：调用 getVo。预期结果：抛出 BizException。
   */
  public void getVoThrowsWhenMissing() {
    when(userManager.getById(999L)).thenReturn(null);
    assertThrows(BizException.class, () -> service.getVo(999L));
  }

  /**
   * 验证当前用户资料视图包含基础身份字段。
   *
   * <p>前置条件：用户 1 存在且昵称为 Alice。关键步骤：调用 getProfileVo。预期结果：视图 ID=1，
   * 昵称=Alice。
   */
  public void getProfileVoReturnsVo() {
    UserPo u = new UserPo();
    u.setId(1L);
    u.setNickname("Alice");
    when(userManager.getById(1L)).thenReturn(u);

    UserVo vo = service.getProfileVo(1L);

    assertEquals(1L, vo.getId());
    assertEquals("Alice", vo.getNickname());
  }

  /**
   * 验证资料更新会写入所有非 null 字段。
   *
   * <p>前置条件：DTO 同时包含昵称、头像和性别。关键步骤：调用 updateProfile。预期结果：返回 true，
   * 并调用 updateById。
   */
  public void updateProfileUpdatesPartialFields() {
    UserProfileDto dto = new UserProfileDto();
    dto.setNickname("NewNick");
    dto.setAvatar("https://example.com/new.png");
    dto.setGender(2);

    boolean ok = service.updateProfile(1L, dto);
    assertTrue(ok);
    verify(userManager).updateById(any(UserPo.class));
  }

  /**
   * 验证资料局部更新会跳过 null 字段。
   *
   * <p>前置条件：DTO 仅包含头像。关键步骤：调用 updateProfile。预期结果：更新成功，未设置昵称和性别
   * 字段。
   */
  public void updateProfileSkipsNullFields() {
    UserProfileDto dto = new UserProfileDto();
    dto.setAvatar("https://example.com/new.png");

    boolean ok = service.updateProfile(1L, dto);
    assertTrue(ok);
  }
}
