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
 * @Description: MerchantNotifyControllerV1 控制器，提供相关接口。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@RestController
@RequestMapping("/api/v1/merchant/notify")
public class MerchantNotifyControllerV1 {

    private final NotifyService notifyService;

    /**
     * 构造 MerchantNotifyControllerV1 实例。
     * @param notifyService notifyService 参数
     */
    public MerchantNotifyControllerV1(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    /**
     * 分页查询数据。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 统一响应结果
     */
    @GetMapping("/messages")
    public Result<CursorPageVO<NotifyMessageVO>> page(@RequestHeader("X-Merchant-Id") Long merchantId,
                                                      @Valid NotifyListDTO dto) {
        dto.setUserId(merchantId);
        dto.setRecipientType(RecipientTypeEnum.MERCHANT.intCode());
        return Result.success(notifyService.pageVO(dto));
    }


    /**
     * 执行 unreadCount 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @GetMapping("/messages/_count")
    public Result<Long> unreadCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
        return Result.success(notifyService.unreadCount(
            RecipientTypeEnum.MERCHANT.intCode(), merchantId));
    }


    /**
     * 执行 markRead 方法。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @PostMapping("/messages/{id}/_read")
    public Result<Void> markRead(@PathVariable Long id,
                                 @RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.markRead(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }


    /**
     * 执行 markAllRead 方法。
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @PostMapping("/messages/_read-all")
    public Result<Void> markAllRead(@RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.markAllRead(RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }


    /**
     * 删除业务数据。
     * @param id id 参数
     * @param merchantId merchantId 参数
     * @return 统一响应结果
     */
    @DeleteMapping("/messages/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-Merchant-Id") Long merchantId) {
        notifyService.delete(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
        return Result.success(null);
    }
}
