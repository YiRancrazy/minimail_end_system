package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.user.UserManageVo;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户管理内部服务入口，向平台侧提供游标分页和用户管理详情查询，并复用用户服务的视图转换与脱敏结果.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/user/manage")
public class InternalUserManageControllerV1 {

  private final UserService userService;

  /**
   * 注入用户业务服务。
   *
   * @param userService 用户服务
   */
  public InternalUserManageControllerV1(UserService userService) {
    this.userService = userService;
  }

  /**
   * 为平台管理场景按游标分页查询用户。
   *
   * @param query 游标、每页数量和可选昵称关键字
   * @return 已脱敏的用户管理视图分页结果
   */
  @GetMapping
  public Result<CursorPageVo<UserManageVo>> page(InternalPageQuery query) {
    return Result.success(userService.pageManage(query));
  }

  /**
   * 查询指定用户的管理详情。
   *
   * @param userId 目标用户 ID
   * @return 用户管理视图
   * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
   */
  @GetMapping("/{userId}")
  public Result<UserManageVo> detail(@PathVariable("userId") Long userId) {
    return Result.success(userService.getManageDetail(userId));
  }
}
