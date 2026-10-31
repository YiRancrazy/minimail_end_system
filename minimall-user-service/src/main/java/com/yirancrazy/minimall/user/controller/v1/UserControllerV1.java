package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.dto.UserPageDto;
import com.yirancrazy.minimall.user.dto.UserProfileDto;
import com.yirancrazy.minimall.user.dto.UserUpdateDto;
import com.yirancrazy.minimall.user.service.UserService;
import com.yirancrazy.minimall.user.vo.UserVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户 HTTP 入口，平台管理接口要求 PLATFORM 角色，用户资料接口仅从 X-User-Id 请求头读取当前用户身份.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/users")
public class UserControllerV1 {

  private final UserService userService;

  /**
   * 注入用户业务服务。
   *
   * @param userService 用户服务
   */
  public UserControllerV1(UserService userService) {
    this.userService = userService;
  }

  /**
   * 平台分页查询用户列表。
   *
   * <p>调用前校验 X-User-Role 必须为 PLATFORM。
   *
   * @param dto 游标、每页数量和昵称关键字
   * @param role 网关注入的用户角色
   * @return 脱敏用户视图分页结果
   * @throws BizException 角色不是 PLATFORM 时抛出
   */
  @GetMapping
  public Result<CursorPageVo<UserVo>> page(
      @Valid UserPageDto dto, @RequestHeader("X-User-Role") String role) {
    requirePlatform(role);
    return Result.success(userService.pageVo(dto));
  }

  /**
   * 平台查询指定用户详情。
   *
   * @param id 用户 ID
   * @param role 网关注入的用户角色
   * @return 脱敏用户视图
   * @throws BizException 角色不是 PLATFORM 或用户不存在时抛出
   */
  @GetMapping("/{id}")
  public Result<UserVo> get(
      @PathVariable("id") Long id, @RequestHeader("X-User-Role") String role) {
    requirePlatform(role);
    return Result.success(userService.getVo(id));
  }

  /**
   * 平台更新指定用户资料。
   *
   * @param id 用户 ID
   * @param dto 昵称、手机号和邮箱更新参数
   * @param role 网关注入的用户角色
   * @return 是否成功更新
   * @throws BizException 角色不是 PLATFORM 时抛出
   */
  @PutMapping("/{id}")
  public Result<Boolean> update(
      @PathVariable("id") Long id,
      @Valid @RequestBody UserUpdateDto dto,
      @RequestHeader("X-User-Role") String role) {
    requirePlatform(role);
    return Result.success(userService.update(id, dto));
  }

  /**
   * 平台删除指定用户。
   *
   * @param id 用户 ID
   * @param role 网关注入的用户角色
   * @return 是否成功删除
   * @throws BizException 角色不是 PLATFORM 时抛出
   */
  @DeleteMapping("/{id}")
  public Result<Boolean> delete(
      @PathVariable("id") Long id, @RequestHeader("X-User-Role") String role) {
    requirePlatform(role);
    return Result.success(userService.delete(id));
  }

  /**
   * 校验请求角色是否具备平台管理权限。
   *
   * @param role 网关注入的角色编码
   * @throws BizException 角色不等于 PLATFORM 时抛出 FORBIDDEN
   */
  private void requirePlatform(String role) {
    if (!RoleEnum.PLATFORM.getCode().equals(role)) {
      throw new BizException(CommonCode.FORBIDDEN, "FORBIDDEN", "无权限访问");
    }
  }

  /**
   * 查询当前用户资料。
   *
   * @param userId 从 X-User-Id 获取的当前用户 ID
   * @return 已完成敏感信息脱敏和头像 URL 解析的用户视图
   * @throws BizException 用户不存在时抛出
   */
  @GetMapping("/profile")
  public Result<UserVo> getProfile(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(userService.getProfileVo(userId));
  }

  /**
   * 局部更新当前用户资料。
   *
   * <p>仅更新请求中非 null 的昵称、头像和性别字段。
   *
   * @param userId 从 X-User-Id 获取的当前用户 ID
   * @param dto 资料局部更新参数
   * @return 是否成功更新
   */
  @PatchMapping("/profile")
  public Result<Boolean> updateProfile(
      @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody UserProfileDto dto) {
    return Result.success(userService.updateProfile(userId, dto));
  }
}
