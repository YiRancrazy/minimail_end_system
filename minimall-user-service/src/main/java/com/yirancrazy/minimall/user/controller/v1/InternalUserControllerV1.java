package com.yirancrazy.minimall.user.controller.v1;

import com.yirancrazy.minimall.api.dto.user.UserProvisionDto;
import com.yirancrazy.minimall.api.dto.user.UserSnapshotDto;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户内部服务入口，提供账号建档和用户快照查询，供认证等服务通过 Feign 调用；当前路径不得直接暴露公网.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/user")
public class InternalUserControllerV1 {

  private final UserService userService;

  /**
   * 注入用户业务服务。
   *
   * @param userService 用户服务
   */
  public InternalUserControllerV1(UserService userService) {
    this.userService = userService;
  }

  /**
   * 接收认证侧建档请求并为指定业务用户补齐本地档案。
   *
   * <p>重复请求由用户服务按幂等语义处理。
   *
   * @param dto 包含业务用户 ID 的建档请求
   * @return data 为 null 的成功响应
   */
  @PostMapping("/provision")
  public Result<Void> provision(@Valid @RequestBody UserProvisionDto dto) {
    userService.provision(dto.getUserId());
    return Result.success();
  }

  /**
   * 查询内部服务所需的用户快照。
   *
   * @param id 用户 ID
   * @return 包含用户 ID、昵称和 USER 角色编码的快照
   * @throws com.yirancrazy.minimall.common.exception.BizException 用户不存在时抛出
   */
  @GetMapping("/{id}")
  public Result<UserSnapshotDto> snapshot(@PathVariable Long id) {
    return Result.success(userService.getSnapshot(id));
  }
}
