package com.yirancrazy.minimall.platform.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.platform.dto.PlatformUserPageDto;
import com.yirancrazy.minimall.platform.service.PlatformUserService;
import com.yirancrazy.minimall.platform.vo.PlatformUserVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端用户管理接口，提供用户列表与详情查询，并返回脱敏后的展示数据.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/users")
public class PlatformUserControllerV1 {

  private final PlatformUserService platformUserService;

  /**
   * 注入平台用户服务，构造用户管理接口.
   *
   * @param platformUserService 平台用户业务服务
   */
  public PlatformUserControllerV1(PlatformUserService platformUserService) {
    this.platformUserService = platformUserService;
  }

  /**
   * 按游标和关键词查询平台用户管理列表.
   *
   * @param dto 用户分页筛选条件
   * @return 用户游标分页数据
   */
  @GetMapping
  public Result<CursorPageVo<PlatformUserVo>> page(@Valid PlatformUserPageDto dto) {
    return Result.success(platformUserService.page(dto));
  }

  /**
   * 查询指定平台用户的详情.
   *
   * @param userId 用户 ID
   * @return 用户详情视图
   * @throws com.yirancrazy.minimall.common.exception.BizException 用户服务未返回用户数据时抛出
   */
  @GetMapping("/{userId}")
  public Result<PlatformUserVo> detail(@PathVariable("userId") Long userId) {
    return Result.success(platformUserService.detail(userId));
  }
}
