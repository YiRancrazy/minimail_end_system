package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.constant.RecipientTypeEnum;
import com.yirancrazy.minimall.notify.dto.NotifyBatchDeleteDto;
import com.yirancrazy.minimall.notify.dto.NotifyListDto;
import com.yirancrazy.minimall.notify.service.NotifyService;
import com.yirancrazy.minimall.notify.vo.NotifyMessageVo;
import jakarta.validation.Valid;
import java.util.List;
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
 * @Description: 提供用户站内通知列表、游标分页、未读计数、已读、单条删除和批量删除接口，接收方身份固定为 X-User-Id 对应的 USER.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/notify")
public class UserNotifyControllerV1 {

  private final NotifyService notifyService;

  /**
   * 注入站内通知业务服务.
   *
   * @param notifyService 用户通知查询和状态变更入口
   */
  public UserNotifyControllerV1(NotifyService notifyService) {
    this.notifyService = notifyService;
  }

  /**
   * 查询当前用户通知列表，并强制请求头用户编号和 USER 接收方类型.
   *
   * @param userId 当前登录用户编号
   * @param dto 消息类型和已读状态筛选条件
   * @return 按主键倒序排列的通知视图
   */
  @GetMapping
  public Result<List<NotifyMessageVo>> list(
      @RequestHeader("X-User-Id") Long userId, @Valid NotifyListDto dto) {
    dto.setUserId(userId);
    dto.setRecipientType(RecipientTypeEnum.USER.intCode());
    return Result.success(notifyService.listByUserVo(dto));
  }

  /**
   * 查询当前用户通知游标分页，并合并 USER 类型消息和全端公告占位消息.
   *
   * @param userId 当前登录用户编号
   * @param dto 游标、消息类型和已读状态筛选条件
   * @return 当前页通知视图及下一页游标
   */
  @GetMapping("/messages")
  public Result<CursorPageVo<NotifyMessageVo>> page(
      @RequestHeader("X-User-Id") Long userId, @Valid NotifyListDto dto) {
    dto.setUserId(userId);
    dto.setRecipientType(RecipientTypeEnum.USER.intCode());
    return Result.success(notifyService.pageVo(dto));
  }

  /**
   * 统计当前用户未读站内通知数量.
   *
   * @param userId 当前登录用户编号
   * @return 未读通知数量
   */
  @GetMapping("/messages/_count")
  public Result<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
    return Result.success(notifyService.unreadCount(RecipientTypeEnum.USER.intCode(), userId));
  }

  /**
   * 兼容旧路径的未读计数别名，委托给 /messages/_count 对应逻辑.
   *
   * @param userId 当前登录用户编号
   * @return 未读通知数量
   */
  @GetMapping("/unread-count")
  public Result<Long> unreadCountAlias(@RequestHeader("X-User-Id") Long userId) {
    return unreadCount(userId);
  }

  /**
   * 将当前用户拥有的指定通知标记为已读.
   *
   * @param id 通知主键
   * @param userId 当前登录用户编号
   * @return 无业务数据的成功响应
   */
  @PostMapping("/messages/{id}/_read")
  public Result<Void> markRead(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
    notifyService.markRead(id, RecipientTypeEnum.USER.intCode(), userId);
    return Result.success(null);
  }

  /**
   * 将当前用户的全部未读通知标记为已读.
   *
   * @param userId 当前登录用户编号
   * @return 无业务数据的成功响应
   */
  @PostMapping("/messages/_read-all")
  public Result<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
    notifyService.markAllRead(RecipientTypeEnum.USER.intCode(), userId);
    return Result.success(null);
  }

  /**
   * 删除当前用户拥有的指定通知.
   *
   * @param id 通知主键
   * @param userId 当前登录用户编号
   * @return 无业务数据的成功响应
   */
  @DeleteMapping("/messages/{id}")
  public Result<Void> delete(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
    notifyService.delete(id, RecipientTypeEnum.USER.intCode(), userId);
    return Result.success(null);
  }

  /**
   * 批量删除当前用户的通知，服务层限制 1 至 100 条并校验全部归属.
   *
   * @param userId 当前登录用户编号
   * @param dto 待删除通知编号集合
   * @return 无业务数据的成功响应
   */
  @DeleteMapping("/messages/batch")
  public Result<Void> batchDelete(
      @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody NotifyBatchDeleteDto dto) {
    notifyService.batchDelete(dto.getIds(), RecipientTypeEnum.USER.intCode(), userId);
    return Result.success(null);
  }
}
