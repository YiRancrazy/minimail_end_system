package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.PreferenceBatchUpdateDto;
import com.yirancrazy.minimall.notify.service.PreferenceService;
import com.yirancrazy.minimall.notify.vo.PreferenceVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户通知偏好查询和批量更新接口，用户身份统一取自 X-User-Id 请求头.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/preferences")
public class UserPreferenceControllerV1 {

  private final PreferenceService preferenceService;

  /**
   * 注入通知偏好业务服务.
   *
   * @param preferenceService 偏好查询和更新入口
   */
  public UserPreferenceControllerV1(PreferenceService preferenceService) {
    this.preferenceService = preferenceService;
  }

  /**
   * 查询当前用户全部通知类别偏好，未保存类别由服务层补全为默认启用.
   *
   * @param userId 当前登录用户编号
   * @return 全部通知类别偏好列表
   */
  @GetMapping
  public Result<List<PreferenceVo>> list(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(preferenceService.listByUser(userId));
  }

  /**
   * 批量更新当前用户的站点、短信和邮件通知开关.
   *
   * @param userId 当前登录用户编号
   * @param dto 待更新的偏好项集合
   * @return 无业务数据的成功响应
   */
  @PostMapping
  public Result<Void> updateBatch(
      @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody PreferenceBatchUpdateDto dto) {
    preferenceService.updateBatch(userId, dto.getItems());
    return Result.success(null);
  }
}
