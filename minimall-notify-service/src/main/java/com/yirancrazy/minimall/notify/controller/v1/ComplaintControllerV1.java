package com.yirancrazy.minimall.notify.controller.v1;

import com.yirancrazy.minimall.common.annotation.Idempotent;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.notify.dto.ComplaintCreateDto;
import com.yirancrazy.minimall.notify.dto.ComplaintHandleDto;
import com.yirancrazy.minimall.notify.dto.ComplaintPageDto;
import com.yirancrazy.minimall.notify.service.ComplaintService;
import com.yirancrazy.minimall.notify.vo.ComplaintVo;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供平台侧投诉创建、游标查询、状态处理详情接口，并统一通过 Result 包装响应；写操作身份取自 X-User-Id 请求头.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/complaints")
public class ComplaintControllerV1 {

  private final ComplaintService complaintService;

  /**
   * 注入投诉业务服务.
   *
   * @param complaintService 投诉创建、查询和处理入口
   */
  public ComplaintControllerV1(ComplaintService complaintService) {
    this.complaintService = complaintService;
  }

  /**
   * 创建一条待处理投诉，投诉人编号取自可信请求头，并启用仓库统一幂等切面.
   *
   * @param complainantId 当前请求用户编号
   * @param dto 投诉双方、关联订单、类型、标题和正文
   * @return 新投诉主键
   */
  @PostMapping
  @Idempotent
  public Result<Long> create(
      @RequestHeader("X-User-Id") Long complainantId, @Valid @RequestBody ComplaintCreateDto dto) {
    return Result.success(complaintService.create(complainantId, dto));
  }

  /**
   * 查询平台投诉游标分页，可使用状态、订单号、投诉人和被投诉人过滤.
   *
   * @param dto 游标及投诉筛选条件
   * @return 当前页投诉视图及下一页游标
   */
  @GetMapping
  public Result<CursorPageVo<ComplaintVo>> page(@Valid ComplaintPageDto dto) {
    return Result.success(complaintService.pageVo(dto));
  }

  /**
   * 处理指定投诉，处理人编号取自请求头，目标状态与结果由服务层校验状态机.
   *
   * @param id 投诉主键
   * @param handlerId 当前平台处理人编号
   * @param dto 目标状态和处理结果
   * @return 无业务数据的成功响应
   */
  @PostMapping("/{id}/handle")
  public Result<Void> handle(
      @PathVariable("id") Long id,
      @RequestHeader("X-User-Id") Long handlerId,
      @Valid @RequestBody ComplaintHandleDto dto) {
    complaintService.handle(id, handlerId, dto);
    return Result.success(null);
  }

  /**
   * 查询平台侧投诉详情并转换为对外视图.
   *
   * @param id 投诉主键
   * @return 投诉详情视图
   */
  @GetMapping("/{id}")
  public Result<ComplaintVo> detail(@PathVariable("id") Long id) {
    return Result.success(complaintService.detailVo(id));
  }
}
