package com.yirancrazy.minimall.notify.controller.v1;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyBatchDeleteDTO;
import com.yirancrazy.minimall.notify.dto.NotifyBroadcastDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.dto.NotifyMarketingPushDTO;
import com.yirancrazy.minimall.notify.dto.NotifySystemAlertDTO;
import com.yirancrazy.minimall.notify.dto.NotifyViolationWarningDTO;
import com.yirancrazy.minimall.notify.entity.NotifyMessagePO;
import com.yirancrazy.minimall.notify.service.NotifyService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 通知控制器，提供站内信 RESTful API，覆盖用户/商家/平台三端
 * @Version: 1.1
 * @DateTime: 2026/08/03
 */
@RestController
@RequestMapping("/api/v1/notify")
public class NotifyControllerV1 {

    private final NotifyService notifyService;

    public NotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 拉取指定用户的消息列表（向后兼容入口），按ID降序返回全部通知记录。
     *
     * @param dto 查询条件
     * @return 通知消息列表的 Result 包装
     */
    @GetMapping
    public Result<List<NotifyMessagePO>> list(@Valid NotifyListDTO dto) {
        return Result.success(notifyService.listByUser(dto));
    }

    // ==================== USER 端 ====================

    /**
     * 用户分页查询站内信。
     * @param userId 用户ID（Header 注入）
     * @param dto 分页入参
     * @return 站内信分页结果
     */
    @GetMapping("/messages")
    public Result<IPage<NotifyMessagePO>> userPage(@RequestHeader("X-User-Id") Long userId,
                                                   @Valid NotifyListDTO dto) {
        dto.setUserId(userId);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        return Result.success(notifyService.page(dto));
    }

    /**
     * 用户未读消息数量。
     * @param userId 用户ID
     * @return 未读数
     */
    @GetMapping("/messages/unread-count")
    public Result<Long> userUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notifyService.unreadCount(RecipientTypeEnum.USER.intCode(), userId));
    }

    /**
     * 用户标记单条消息为已读。
     * @param id 消息ID
     * @param userId 用户ID
     */
    @PostMapping("/messages/{id}/read")
    public Result<Void> userMarkRead(@PathVariable Long id,
                                     @RequestHeader("X-User-Id") Long userId) {
        notifyService.markRead(id, RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }

    /**
     * 用户全部消息标记为已读。
     * @param userId 用户ID
     */
    @PostMapping("/messages/read-all")
    public Result<Void> userMarkAllRead(@RequestHeader("X-User-Id") Long userId) {
        notifyService.markAllRead(RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }

    /**
     * 用户删除单条消息。
     * @param id 消息ID
     * @param userId 用户ID
     */
    @DeleteMapping("/messages/{id}")
    public Result<Void> userDelete(@PathVariable Long id,
                                   @RequestHeader("X-User-Id") Long userId) {
        notifyService.delete(id, RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }

    /**
     * 用户批量删除消息（软删除），单次最多100条。
     * @param userId 用户ID
     * @param dto 批量删除入参
     * @return 操作结果
     */
    @DeleteMapping("/messages/batch")
    public Result<Void> batchDelete(@RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody NotifyBatchDeleteDTO dto) {
        notifyService.batchDelete(dto.getIds(), RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }

    // ==================== MERCHANT 端 ====================

    /**
     * 商家分页查询站内信。
     * @param merchantId 商家ID（Header 注入）
     * @param dto 分页入参
     * @return 站内信分页结果
     */
    @GetMapping("/merchant/messages")
    public Result<IPage<NotifyMessagePO>> merchantPage(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                       @Valid NotifyListDTO dto) {
        dto.setUserId(merchantId);
        dto.setRecipientType(RecipientTypeEnum.MERCHANT.intCode());
        return Result.success(notifyService.page(dto));
    }

    /**
     * 商家未读消息数量。
     * @param merchantId 商家ID
     * @return 未读数
     */
    @GetMapping("/merchant/messages/unread-count")
    public Result<Long> merchantUnreadCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(notifyService.unreadCount(
            RecipientTypeEnum.MERCHANT.intCode(), merchantId));
    }

    /**
     * 商家标记单条消息为已读。
     * @param id 消息ID
     * @param merchantId 商家ID
     */
    @PostMapping("/merchant/messages/{id}/read")
    public Result<Void> merchantMarkRead(@PathVariable Long id,
                                         @RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.markRead(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }

    /**
     * 商家全部消息标记为已读。
     * @param merchantId 商家ID
     */
    @PostMapping("/merchant/messages/read-all")
    public Result<Void> merchantMarkAllRead(@RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.markAllRead(RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }

    /**
     * 商家删除单条消息。
     * @param id 消息ID
     * @param merchantId 商家ID
     */
    @DeleteMapping("/merchant/messages/{id}")
    public Result<Void> merchantDelete(@PathVariable Long id,
                                       @RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.delete(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }

    // ==================== PLATFORM 端 ====================

    /**
     * 平台分页查询站内信。
     * @param adminId 管理员ID（Header 注入）
     * @param dto 分页入参
     * @return 站内信分页结果
     */
    @GetMapping("/platform/messages")
    public Result<IPage<NotifyMessagePO>> platformPage(@RequestHeader("X-User-Id") Long adminId,
                                                       @Valid NotifyListDTO dto) {
        dto.setUserId(adminId);
        dto.setRecipientType(RecipientTypeEnum.PLATFORM.intCode());
        return Result.success(notifyService.page(dto));
    }

    /**
     * 平台广播消息，支持全端或指定接收者推送。
     * @param adminId 管理员ID
     * @param dto 广播入参
     */
    @PostMapping("/platform/broadcast")
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
    @PostMapping("/platform/marketing-push")
    public Result<Void> marketingPush(@Valid @RequestBody NotifyMarketingPushDTO dto) {
        notifyService.marketingPush(dto);
        return Result.success(null);
    }

    /**
     * 违规警告通知，向指定商家发送违规警告站内信。
     * @param dto 违规警告入参
     * @return 统一响应体
     */
    @PostMapping("/platform/violation-warning")
    public Result<Void> violationWarning(@Valid @RequestBody NotifyViolationWarningDTO dto) {
        notifyService.violationWarning(dto);
        return Result.success(null);
    }

    /**
     * 系统告警通知，向平台管理员发送系统异常/故障预警站内信。
     * @param dto 系统告警入参
     * @return 统一响应体
     */
    @PostMapping("/platform/system-alert")
    public Result<Void> systemAlert(@Valid @RequestBody NotifySystemAlertDTO dto) {
        notifyService.systemAlert(dto);
        return Result.success(null);
    }
}
