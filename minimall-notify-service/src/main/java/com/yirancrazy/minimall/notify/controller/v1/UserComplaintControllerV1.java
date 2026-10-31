package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDto;
import com.yirancrazy.minimall.notify.service.ComplaintService;
import com.yirancrazy.minimall.notify.vo.ComplaintVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供用户查询本人投诉列表和详情的只读接口，并通过请求头用户编号限制投诉归属.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/user/complaints")
public class UserComplaintControllerV1 {

  private final ComplaintService complaintService;

  /**
   * 注入投诉业务服务.
   *
   * @param complaintService 用户投诉查询入口
   */
  public UserComplaintControllerV1(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  /**
   * 查询当前用户投诉游标分页，并强制用请求头用户编号覆盖请求中的投诉人筛选值.
   *
   * @param userId 当前登录用户编号
   * @param dto 游标及其他投诉筛选条件
   * @return 当前页投诉视图及下一页游标
   */
  @GetMapping
  public Result<CursorPageVo<ComplaintVo>> page(
      @RequestHeader("X-User-Id") Long userId, @Valid ComplaintPageDto dto) {
    // 强制覆盖 userId 字段，防止前端传入他人 ID 越权查询
    dto.setUserId(userId);
    return Result.success(complaintService.pageVo(dto));
  }

  /**
   * 查询当前用户作为投诉人的指定投诉详情；非本人记录由服务层按不存在处理.
   *
   * @param id 投诉主键
   * @param userId 当前登录用户编号
   * @return 用户侧投诉详情
   */
  @GetMapping("/{id}")
  public Result<ComplaintVo> detail(
      @PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId) {
    return Result.success(complaintService.detailForUser(id, userId));
  }
}
