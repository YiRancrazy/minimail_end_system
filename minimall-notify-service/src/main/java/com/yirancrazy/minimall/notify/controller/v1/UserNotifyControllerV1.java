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
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyBatchDeleteDTO;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户端通知控制器，提供站内信列表、分页查询、未读计数、标记已读与删除能力。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@RestController
@RequestMapping("/api/v1/user/notify")
public class UserNotifyControllerV1 {

    private final NotifyService notifyService;

    public UserNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 拉取指定用户的消息列表（向后兼容入口），按ID降序返回全部通知记录。
     * @param dto 查询条件
     * @return 通知消息列表的 Result 包装
     */
    @GetMapping
    public Result<List<NotifyMessageVO>> list(@Valid NotifyListDTO dto) {
        return Result.success(notifyService.listByUser(dto).stream().map(NotifyMessageVO::from).toList());
    }

    /**
     * 用户分页查询站内信。
     * @param userId 用户ID（Header 注入）
     * @param dto 分页入参
     * @return 站内信分页结果
     */
    @GetMapping("/messages")
    public Result<CursorPageVO<NotifyMessageVO>> page(@RequestHeader("X-User-Id") Long userId,
                                                      @Valid NotifyListDTO dto) {
        dto.setUserId(userId);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        return Result.success(notifyService.page(dto).map(NotifyMessageVO::from));
    }

    /**
     * 用户未读消息数量。
     * @param userId 用户ID
     * @return 未读数
     */
    @GetMapping("/messages/_count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notifyService.unreadCount(RecipientTypeEnum.USER.intCode(), userId));
    }

    /**
     * Alias for {@link #unreadCount} that matches the frontend contract.
     */
    @GetMapping("/unread-count")
    public Result<Long> unreadCountAlias(@RequestHeader("X-User-Id") Long userId) {
        return unreadCount(userId);
    }

    /**
     * 用户标记单条消息为已读。
     * @param id 消息ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/messages/{id}/_read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader("X-User-Id") Long userId) {
        notifyService.markRead(id, RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }

    /**
     * 用户全部消息标记为已读。
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/messages/_read-all")
    public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        notifyService.markAllRead(RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }

    /**
     * 用户删除单条消息。
     * @param id 消息ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/messages/{id}")
    public Result<Void> delete(@PathVariable Long id,
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
}
