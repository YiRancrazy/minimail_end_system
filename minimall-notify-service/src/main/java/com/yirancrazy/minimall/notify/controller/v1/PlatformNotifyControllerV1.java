package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: PlatformNotifyControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/platform/notify")
public class PlatformNotifyControllerV1 {

    private final NotifyService notifyService;

    /**
     * 构造 PlatformNotifyControllerV1 实例。
     * @param notifyService notifyService 参数
     */
    public PlatformNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    /**
     * 分页查询数据。
     * @param adminId adminId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/messages")
    public Result<CursorPageVO<NotifyMessageVO>> page(@RequestHeader("X-User-Id") Long adminId,
                                                      @Valid NotifyListDTO dto) {
        dto.setUserId(adminId);
        dto.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
        return Result.success(notifyService.pageVO(dto));
    }


    /**
     * 执行 broadcast 方法。
     * @param adminId adminId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/broadcast")
    public Result<Void> broadcast(@RequestHeader("X-User-Id") Long adminId,
                                  @Valid @RequestBody NotifyBroadcastDTO dto) {
        dto.setSenderId(adminId);
        notifyService.broadcast(dto);
        return Result.success(null);
    }


    /**
     * 执行 marketingPush 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/marketing-push")
    public Result<Void> marketingPush(@Valid @RequestBody NotifyMarketingPushDTO dto) {
        notifyService.marketingPush(dto);
        return Result.success(null);
    }


    /**
     * 执行 violationWarning 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/violation-warning")
    public Result<Void> violationWarning(@Valid @RequestBody NotifyViolationWarningDTO dto) {
        notifyService.violationWarning(dto);
        return Result.success(null);
    }


    /**
     * 执行 systemAlert 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/system-alert")
    public Result<Void> systemAlert(@Valid @RequestBody NotifySystemAlertDTO dto) {
        notifyService.systemAlert(dto);
        return Result.success(null);
    }


    /**
     * 执行 alerts 方法。
     * @param adminId adminId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/alerts")
    public Result<CursorPageVO<NotifyMessageVO>> alerts(@RequestHeader("X-User-Id") Long adminId,
                                                        @Valid SystemAlertPageDTO dto) {
        return Result.success(notifyService.alertPageVO(dto));
    }


    /**
     * 执行 publishAnnouncement 方法。
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @PostMapping("/announcements")
    @RequirePermission(PermissionEnum.NOTIFY_BROADCAST)
    public Result<Void> publishAnnouncement(@Valid @RequestBody AnnouncementCreateDTO dto) {
        notifyService.publishAnnouncement(dto);
        return Result.success(null);
    }


    /**
     * 执行 unreadCount 方法。
     * @param adminId adminId 参数
     * @return 统一响应结果
     */
    @GetMapping("/messages/_count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long adminId) {
        return Result.success(notifyService.unreadCount(
            RecipientTypeEnum.PLATFORM.intCode(), adminId));
    }


    /**
     * 执行 markRead 方法。
     * @param id id 参数
     * @param adminId adminId 参数
     * @return 统一响应结果
     */
    @PostMapping("/messages/{id}/_read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader("X-User-Id") Long adminId) {
        notifyService.markRead(id, RecipientTypeEnum.PLATFORM.intCode(), adminId);
        return Result.success(null);
    }


    /**
     * 执行 markAllRead 方法。
     * @param adminId adminId 参数
     * @return 统一响应结果
     */
    @PostMapping("/messages/_read-all")
    public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long adminId) {
        notifyService.markAllRead(RecipientTypeEnum.PLATFORM.intCode(), adminId);
        return Result.success(null);
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param adminId adminId 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/messages/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long adminId) {
        notifyService.delete(id, RecipientTypeEnum.PLATFORM.intCode(), adminId);
        return Result.success(null);
    }
}
