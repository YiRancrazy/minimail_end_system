package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDto;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDto;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDto;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDto;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDto;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台通知查询、广播、营销、违规告警、系统告警、公告发布及消息已读删除接口；管理身份取自 X-User-Id 请求头.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/notify")
public class PlatformNotifyControllerV1 {

  private final NotifyService notifyService;

  /**
   * 注入站内通知业务服务.
   *
   * @param notifyService 平台通知管理入口
   */
  public PlatformNotifyControllerV1(NotifyService notifyService) {
    this.notifyService = notifyService;
  }

  /**
   * 查询当前平台管理员的接收方通知游标分页，接收方类型固定为 PLATFORM.
   *
   * @param adminId 当前平台用户编号
   * @param dto 游标、消息类型和已读状态筛选条件
   * @return 当前页平台通知及下一页游标
   */
  @GetMapping("/messages")
  public Result<CursorPageVo<NotifyMessageVo>> page(
      @RequestHeader("X-User-Id") Long adminId, @Valid NotifyListDto dto) {
    dto.setUserId(adminId);
    dto.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
    return Result.success(notifyService.pageVo(dto));
  }

  /**
   * 发送平台广播，并把请求头中的管理员编号写入发送方字段.
   *
   * @param adminId 当前平台用户编号
   * @param dto 广播接收方、消息类型、内容和可选定向目标
   * @return 无业务数据的成功响应
   */
  @PostMapping("/broadcast")
  public Result<Void> broadcast(
      @RequestHeader("X-User-Id") Long adminId, @Valid @RequestBody NotifyBroadcastDto dto) {
    dto.setSenderId(adminId);
    notifyService.broadcast(dto);
    return Result.success(null);
  }

  /**
   * 向指定用户批量推送营销通知；目标列表为空时保存全端营销占位消息.
   *
   * @param dto 营销标题、正文和可选用户列表
   * @return 无业务数据的成功响应
   */
  @PostMapping("/marketing-push")
  public Result<Void> marketingPush(@Valid @RequestBody NotifyMarketingPushDto dto) {
    notifyService.marketingPush(dto);
    return Result.success(null);
  }

  /**
   * 向指定商家发送违规警告并实时推送.
   *
   * @param dto 商家编号、警告内容和违规类型
   * @return 无业务数据的成功响应
   */
  @PostMapping("/violation-warning")
  public Result<Void> violationWarning(@Valid @RequestBody NotifyViolationWarningDto dto) {
    notifyService.violationWarning(dto);
    return Result.success(null);
  }

  /**
   * 写入一条平台系统告警消息.
   *
   * @param dto 告警标题、正文和级别
   * @return 无业务数据的成功响应
   */
  @PostMapping("/system-alert")
  public Result<Void> systemAlert(@Valid @RequestBody NotifySystemAlertDto dto) {
    notifyService.systemAlert(dto);
    return Result.success(null);
  }

  /**
   * 查询平台系统告警的游标分页；请求头 adminId 当前不参与查询条件.
   *
   * @param adminId 当前平台用户编号
   * @param dto 告警列表游标分页条件
   * @return 当前页系统告警及下一页游标
   */
  @GetMapping("/alerts")
  public Result<CursorPageVo<NotifyMessageVo>> alerts(
      @RequestHeader("X-User-Id") Long adminId, @Valid SystemAlertPageDto dto) {
    return Result.success(notifyService.alertPageVo(dto));
  }

  /**
   * 发布全端公告；接口要求调用者具备通知广播权限.
   *
   * @param dto 公告标题和正文
   * @return 无业务数据的成功响应
   */
  @PostMapping("/announcements")
  @RequirePermission(PermissionEnum.NOTIFY_BROADCAST)
  public Result<Void> publishAnnouncement(@Valid @RequestBody AnnouncementCreateDto dto) {
    notifyService.publishAnnouncement(dto);
    return Result.success(null);
  }

  /**
   * 统计当前平台管理员未读通知数量.
   *
   * @param adminId 当前平台用户编号
   * @return 未读通知数量
   */
  @GetMapping("/messages/_count")
  public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long adminId) {
    return Result.success(notifyService.unreadCount(RecipientTypeEnum.PLATFORM.intCode(), adminId));
  }

  /**
   * 将当前平台管理员拥有的指定通知标记为已读.
   *
   * @param id 通知主键
   * @param adminId 当前平台用户编号
   * @return 无业务数据的成功响应
   */
  @PostMapping("/messages/{id}/_read")
  public Result<Void> markRead(@PathVariable Long id, @RequestHeader("X-User-Id") Long adminId) {
    notifyService.markRead(id, RecipientTypeEnum.PLATFORM.intCode(), adminId);
    return Result.success(null);
  }

  /**
   * 将当前平台管理员的全部未读通知标记为已读.
   *
   * @param adminId 当前平台用户编号
   * @return 无业务数据的成功响应
   */
  @PostMapping("/messages/_read-all")
  public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long adminId) {
    notifyService.markAllRead(RecipientTypeEnum.PLATFORM.intCode(), adminId);
    return Result.success(null);
  }

  /**
   * 删除当前平台管理员拥有的指定通知.
   *
   * @param id 通知主键
   * @param adminId 当前平台用户编号
   * @return 无业务数据的成功响应
   */
  @DeleteMapping("/messages/{id}")
  public Result<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long adminId) {
    notifyService.delete(id, RecipientTypeEnum.PLATFORM.intCode(), adminId);
    return Result.success(null);
  }
}
