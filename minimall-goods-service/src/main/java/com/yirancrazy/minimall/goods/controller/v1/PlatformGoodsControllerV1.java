package com.yirancrazy.minimall.goods.controller.v1;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.goods.dto.SpuPageDto;
import com.yirancrazy.minimall.goods.dto.SpuRejectDto;
import com.yirancrazy.minimall.goods.service.SpuService;
import com.yirancrazy.minimall.goods.vo.SpuAuditRecordVo;
import com.yirancrazy.minimall.goods.vo.SpuVo;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 面向平台管理端提供待审商品分页、详情、审核记录及通过、驳回操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/api/v1/platform/goods")
public class PlatformGoodsControllerV1 {

  private final SpuService spuService;

  /**
   * 注入SPU 聚合服务，构造 PlatformGoodsControllerV1 实例。
   *
   * @param spuService SPU 聚合服务
   */
  public PlatformGoodsControllerV1(SpuService spuService) {
    this.spuService = spuService;
  }

  /**
   * 分页查询待平台审核的 SPU，并转换为视图对象。
   *
   * @param dto SPU 游标分页或状态过滤参数
   * @return 统一响应包装；data 为按主键游标分页的结果；记录项为转换后的 SPU 视图对象
   */
  @GetMapping("/spus/pending")
  public Result<CursorPageVo<SpuVo>> pending(@Valid SpuPageDto dto) {
    return Result.success(spuService.pendingVo(dto));
  }

  /**
   * 平台按 SPU 主键查看商品主数据和 SKU。
   *
   * @param spuId SPU 主键
   * @return 统一响应包装；data 为转换后的 SPU 视图对象
   */
  @GetMapping("/spus/{spuId}")
  public Result<SpuVo> detail(@PathVariable("spuId") Long spuId) {
    return Result.success(spuService.detailVo(spuId));
  }

  /**
   * 查询指定 SPU 的审核记录，按审核时间倒序返回。
   *
   * @param spuId SPU 主键
   * @return 统一响应包装；data 为商品审核记录视图对象列表；无数据时返回空列表
   */
  @GetMapping("/spus/{spuId}/audit-logs")
  public Result<List<SpuAuditRecordVo>> auditLogs(@PathVariable("spuId") Long spuId) {
    return Result.success(spuService.listAuditRecordsVo(spuId));
  }

  /**
   * 平台审核通过待审 SPU，审核人取自可信请求头。
   *
   * @param spuId SPU 主键
   * @param auditorId 平台审核人用户 ID
   * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
   */
  @PostMapping("/spus/{spuId}/approve")
  public Result<Boolean> approve(
      @PathVariable("spuId") Long spuId, @RequestHeader("X-User-Id") Long auditorId) {
    return Result.success(spuService.approve(spuId, auditorId));
  }

  /**
   * 平台驳回待审 SPU，审核人取自可信请求头并使用请求体中的驳回原因。
   *
   * @param spuId SPU 主键
   * @param auditorId 平台审核人用户 ID
   * @param dto 商品审核驳回参数
   * @return 统一响应包装；data 为操作实际影响记录时为 true，否则为 false
   */
  @PostMapping("/spus/{spuId}/reject")
  public Result<Boolean> reject(
      @PathVariable("spuId") Long spuId,
      @RequestHeader("X-User-Id") Long auditorId,
      @Valid @RequestBody SpuRejectDto dto) {
    return Result.success(spuService.reject(spuId, auditorId, dto.getReason()));
  }

  /**
   * 查询指定 SPU 的审核记录，与审核日志接口共用同一视图。
   *
   * @param spuId SPU 主键
   * @return 统一响应包装；data 为商品审核记录视图对象列表；无数据时返回空列表
   */
  @GetMapping("/spus/{spuId}/audit-records")
  public Result<List<SpuAuditRecordVo>> auditRecords(@PathVariable("spuId") Long spuId) {
    return Result.success(spuService.listAuditRecordsVo(spuId));
  }
}
