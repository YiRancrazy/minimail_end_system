package com.yirancrazy.minimall.user.controller.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 内部用户管理控制器单元测试，使用 Mockito 替身覆盖分页空结果、关键字查询、详情和不存在场景.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public class InternalUserManageControllerV1Test {

  private UserService userService;
  private InternalUserManageControllerV1 controller;

  /**
   * 初始化用户服务替身和待测控制器。
   */
  @BeforeEach
  void setUp() {
    userService = mock(UserService.class);
    controller = new InternalUserManageControllerV1(userService);
  }

  /**
   * 验证服务返回空记录时分页接口保留成功响应。
   *
   * <p>前置条件：UserService 返回空游标页。关键步骤：以 limit=20 调用 page。预期结果：响应码为
   * 00000，records 为空。
   */
  public void pageReturnsEmptyWhenNoRecords() {
    when(userService.pageManage(any(InternalPageQuery.class)))
        .thenReturn(CursorPageVo.of(java.util.List.of(), 20, UserManageVo::getId));

    InternalPageQuery query = new InternalPageQuery();
    query.setLimit(20);
    Result<CursorPageVo<UserManageVo>> r = controller.page(query);

    assertEquals("00000", r.getCode());
    assertEquals(0, r.getData().getRecords().size());
  }

  /**
   * 验证分页请求携带关键字时返回服务提供的用户管理视图。
   *
   * <p>前置条件：关键字为 alice，服务返回一条昵称为“小荷”的记录。关键步骤：调用 page。预期结果：
   * records 数量为 1，昵称保持“小荷”。
   */
  public void pageDelegatesWithKeyword() {
    UserManageVo user = buildUser(1L, "小荷");
    when(userService.pageManage(any(InternalPageQuery.class)))
        .thenReturn(CursorPageVo.of(java.util.List.of(user), 20, UserManageVo::getId));

    InternalPageQuery query = new InternalPageQuery();
    query.setLimit(20);
    query.setKeyword("alice");
    Result<CursorPageVo<UserManageVo>> r = controller.page(query);

    assertEquals(1, r.getData().getRecords().size());
    assertEquals("小荷", r.getData().getRecords().get(0).getNickname());
  }

  /**
   * 验证用户详情接口返回服务组装的管理视图。
   *
   * <p>前置条件：服务对用户 1 返回昵称“小荷”和脱敏手机号。关键步骤：调用 detail(1)。预期结果：
   * 响应中的昵称和脱敏手机号与替身数据一致。
   */
  public void detailReturnsVo() {
    when(userService.getManageDetail(1L)).thenReturn(buildUser(1L, "小荷"));

    Result<UserManageVo> r = controller.detail(1L);

    assertEquals("小荷", r.getData().getNickname());
    assertEquals("138****0001", r.getData().getPhoneMasked());
  }

  /**
   * 验证服务报告用户不存在时详情接口保留业务异常语义。
   *
   * <p>前置条件：服务对用户 99 抛出 USER_NOT_FOUND。关键步骤：调用 detail(99)。预期结果：
   * 向调用方抛出 BizException。
   */
  public void detailThrowsWhenNotFound() {
    when(userService.getManageDetail(99L))
        .thenThrow(
            new BizException(com.yirancrazy.minimall.user.constant.UserCodeEnum.USER_NOT_FOUND));

    assertThrows(BizException.class, () -> controller.detail(99L));
  }

  /**
   * 构造用户管理视图测试数据。
   *
   * @param id 用户 ID
   * @param nickname 用户昵称
   * @return 带有固定脱敏手机号和性别的用户管理视图
   */
  private UserManageVo buildUser(Long id, String nickname) {
    UserManageVo u = new UserManageVo();
    u.setId(id);
    u.setNickname(nickname);
    u.setPhoneMasked("138****0001");
    u.setGender(1);
    return u;
  }
}
