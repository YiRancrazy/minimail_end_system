package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.annotation.RequirePermission;
import com.yirancrazy.minimall.common.constant.PermissionEnum;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.AnnouncementCreateDTO;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.dto.SystemAlertPageDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端通知控制器，提供站内信分页查询、广播、营销推送、违规警告、系统告警与公告发布能力。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@RestController
@RequestMapping("/api/v1/platform/notify")
public class PlatformNotifyControllerV1 {

    private final NotifyService notifyService;

    public PlatformNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 平台分页查询站内信。
     * @param adminId 管理员ID（Header 注入）
     * @param dto 分页入参
     * @return 站内信分页结果
     */
    @GetMapping("/messages")
    public Result<CursorPageVO<NotifyMessagePO>> page(@RequestHeader("X-User-Id") Long adminId,
                                                      @Valid NotifyListDTO dto) {
        dto.setUserId(adminId);
        dto.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
        return Result.success(notifyService.page(dto));
    }

    /**
     * 平台广播消息，支持全端或指定接收者推送。
     * @param adminId 管理员ID
     * @param dto 广播入参
     * @return 操作结果
     */
    @PostMapping("/broadcast")
    public Result<Void> broadcast(@RequestHeader("X-User-Id") Long adminId,
                                  @Valid @RequestBody NotifyBroadcastDTO dto) {
        dto.setSenderId(adminId);
        notifyService.broadcast(dto);
        return Result.success(null);
    }

    /**
     * 营销推送，向指定用户列表或全量用户发送营销站内信。
     * @param dto 营销推送入参
     * @return 统一响应体
     */
    @PostMapping("/marketing-push")
    public Result<Void> marketingPush(@Valid @RequestBody NotifyMarketingPushDTO dto) {
        notifyService.marketingPush(dto);
        return Result.success(null);
    }

    /**
     * 违规警告通知，向指定商家发送违规警告站内信。
     * @param dto 违规警告入参
     * @return 统一响应体
     */
    @PostMapping("/violation-warning")
    public Result<Void> violationWarning(@Valid @RequestBody NotifyViolationWarningDTO dto) {
        notifyService.violationWarning(dto);
        return Result.success(null);
    }

    /**
     * 系统告警通知，向平台管理员发送系统异常/故障预警站内信。
     * @param dto 系统告警入参
     * @return 统一响应体
     */
    @PostMapping("/system-alert")
    public Result<Void> systemAlert(@Valid @RequestBody NotifySystemAlertDTO dto) {
        notifyService.systemAlert(dto);
        return Result.success(null);
    }

    /**
     * 平台系统告警分页查询，固定查询 PLATFORM + SYSTEM 类型消息。
     * @param adminId 管理员ID（Header 注入）
     * @param dto 分页入参
     * @return 系统告警分页结果
     */
    @GetMapping("/alerts")
    public Result<CursorPageVO<NotifyMessagePO>> alerts(@RequestHeader("X-User-Id") Long adminId,
                                                        @Valid SystemAlertPageDTO dto) {
        return Result.success(notifyService.alertPage(dto));
    }

    /**
     * 发布系统公告（平台端）。
     * @param dto 公告发布入参
     * @return 无业务数据的成功响应
     */
    @PostMapping("/announcements")
    @RequirePermission(PermissionEnum.NOTIFY_BROADCAST)
    public Result<Void> publishAnnouncement(@Valid @RequestBody AnnouncementCreateDTO dto) {
        notifyService.publishAnnouncement(dto);
        return Result.success(null);
    }
}
