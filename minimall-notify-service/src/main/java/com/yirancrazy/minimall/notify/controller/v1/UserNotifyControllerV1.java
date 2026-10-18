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
 * @Description: UserNotifyControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/user/notify")
public class UserNotifyControllerV1 {

    private final NotifyService notifyService;

    /**
     * 构造 UserNotifyControllerV1 实例。
     * @param notifyService notifyService 参数
     */
    public UserNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    /**
     * 查询列表数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping
    public Result<List<NotifyMessageVO>> list(@RequestHeader("X-User-Id") Long userId,
                                              @Valid NotifyListDTO dto) {
        dto.setUserId(userId);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        return Result.success(notifyService.listByUserVO(dto));
    }


    /**
     * 分页查询数据。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/messages")
    public Result<CursorPageVO<NotifyMessageVO>> page(@RequestHeader("X-User-Id") Long userId,
                                                      @Valid NotifyListDTO dto) {
        dto.setUserId(userId);
        dto.setRecipientType(RecipientTypeEnum.USER.intCode());
        return Result.success(notifyService.pageVO(dto));
    }


    /**
     * 执行 unreadCount 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/messages/_count")
    public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(notifyService.unreadCount(RecipientTypeEnum.USER.intCode(), userId));
    }


    /**
     * 执行 unreadCountAlias 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @GetMapping("/unread-count")
    public Result<Long> unreadCountAlias(@RequestHeader("X-User-Id") Long userId) {
        return unreadCount(userId);
    }


    /**
     * 执行 markRead 方法。
     * @param id id 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @PostMapping("/messages/{id}/_read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader("X-User-Id") Long userId) {
        notifyService.markRead(id, RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }


    /**
     * 执行 markAllRead 方法。
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @PostMapping("/messages/_read-all")
    public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        notifyService.markAllRead(RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param userId userId 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/messages/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        notifyService.delete(id, RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }


    /**
     * 执行 batchDelete 方法。
     * @param userId userId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/messages/batch")
    public Result<Void> batchDelete(@RequestHeader("X-User-Id") Long userId,
                                    @Valid @RequestBody NotifyBatchDeleteDTO dto) {
        notifyService.batchDelete(dto.getIds(), RecipientTypeEnum.USER.intCode(), userId);
        return Result.success(null);
    }
}
