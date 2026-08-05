package com.yirancrazy.minimall.notify.controller.v1;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyListDTO;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家端通知控制器，提供商家站内信分页查询、未读计数、标记已读与删除能力。
 * @Version: 1.0
 * @DateTime: 2026/08/04
 **/
@RestController
@RequestMapping("/api/v1/merchant/notify")
public class MerchantNotifyControllerV1 {

    private final NotifyService notifyService;

    public MerchantNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /**
     * 商家分页查询站内信。
     * @param merchantId 商家ID（Header 注入）
     * @param dto 分页入参
     * @return 站内信分页结果
     */
    @GetMapping("/messages")
    public Result<CursorPageVO<NotifyMessageVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                      @Valid NotifyListDTO dto) {
        dto.setUserId(merchantId);
        dto.setRecipientType(RecipientTypeEnum.MERCHANT.intCode());
        return Result.success(notifyService.page(dto).map(NotifyMessageVO::from));
    }

    /**
     * 商家未读消息数量。
     * @param merchantId 商家ID
     * @return 未读数
     */
    @GetMapping("/messages/unread-count")
    public Result<Long> unreadCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(notifyService.unreadCount(
            RecipientTypeEnum.MERCHANT.intCode(), merchantId));
    }

    /**
     * 商家标记单条消息为已读。
     * @param id 消息ID
     * @param merchantId 商家ID
     * @return 操作结果
     */
    @PostMapping("/messages/{id}/read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.markRead(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }

    /**
     * 商家全部消息标记为已读。
     * @param merchantId 商家ID
     * @return 操作结果
     */
    @PostMapping("/messages/read-all")
    public Result<Void> markAllRead(@RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.markAllRead(RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }

    /**
     * 商家删除单条消息。
     * @param id 消息ID
     * @param merchantId 商家ID
     * @return 操作结果
     */
    @DeleteMapping("/messages/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.delete(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }
}
