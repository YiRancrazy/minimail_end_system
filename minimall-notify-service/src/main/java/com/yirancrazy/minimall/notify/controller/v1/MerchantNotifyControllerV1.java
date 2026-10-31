package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供商家站内通知的游标查询、未读计数、单条和全部已读、单条删除接口，接收方身份固定来自 X-Merchant-Id.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/merchant/notify")
public class MerchantNotifyControllerV1 {

  private final NotifyService notifyService;

  /**
   * 注入站内通知业务服务.
   *
   * @param notifyService 商家通知查询、已读和删除入口
   */
  public MerchantNotifyControllerV1(NotifyService notifyService) {
    this.notifyService = notifyService;
  }

  /**
   * 查询当前商家的通知游标分页，并把接收方类型固定为 MERCHANT.
   *
   * @param merchantId 当前登录商家编号
   * @param dto 游标、消息类型和已读状态筛选条件
   * @return 当前页商家通知及下一页游标
   */
  @GetMapping("/messages")
  public Result<CursorPageVo<NotifyMessageVo>> page(
      @RequestHeader("X-Merchant-Id") Long merchantId, @Valid NotifyListDto dto) {
    dto.setUserId(merchantId);
    dto.setRecipientType(RecipientTypeEnum.MERCHANT.intCode());
    return Result.success(notifyService.pageVo(dto));
  }

  /**
   * 统计当前商家未读站内通知数量.
   *
   * @param merchantId 当前登录商家编号
   * @return 未读通知数量
   */
  @GetMapping("/messages/_count")
  public Result<Long> unreadCount(@RequestHeader("X-Merchant-Id") Long merchantId) {
    return Result.success(
        notifyService.unreadCount(RecipientTypeEnum.MERCHANT.intCode(), merchantId));
  }

  /**
   * 将当前商家拥有的指定通知标记为已读.
   *
   * @param id 通知主键
   * @param merchantId 当前登录商家编号
   * @return 无业务数据的成功响应
   */
  @PostMapping("/messages/{id}/_read")
  public Result<Void> markRead(
      @PathVariable Long id, @RequestHeader("X-Merchant-Id") Long merchantId) {
    notifyService.markRead(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
    return Result.success(null);
  }

  /**
   * 将当前商家的全部未读通知标记为已读.
   *
   * @param merchantId 当前登录商家编号
   * @return 无业务数据的成功响应
   */
  @PostMapping("/messages/_read-all")
  public Result<Void> markAllRead(@RequestHeader("X-Merchant-Id") Long merchantId) {
    notifyService.markAllRead(RecipientTypeEnum.MERCHANT.intCode(), merchantId);
    return Result.success(null);
  }

  /**
   * 删除当前商家拥有的指定通知，归属不匹配时由服务层拒绝.
   *
   * @param id 通知主键
   * @param merchantId 当前登录商家编号
   * @return 无业务数据的成功响应
   */
  @DeleteMapping("/messages/{id}")
  public Result<Void> delete(
      @PathVariable Long id, @RequestHeader("X-Merchant-Id") Long merchantId) {
    notifyService.delete(id, RecipientTypeEnum.MERCHANT.intCode(), merchantId);
    return Result.success(null);
  }
}
