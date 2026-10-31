package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.common.constant.RoleEnum;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDto;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVo;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVo;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVo;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质 HTTP 接口；面向商家端提供资质提交、详情、概览和审核记录查询，并向平台角色开放审核入口，业务校验委托 MerchantService。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant/merchants")
public class MerchantControllerV1 {

  private final MerchantService merchantService;

  /**
   * 注入商家资质服务。
   *
   * @param merchantService 商家资质领域服务
   */
  public MerchantControllerV1(MerchantService merchantService) {
    this.merchantService = merchantService;
  }

  /**
   * 提交或刷新当前商家的资质材料。
   *
   * 商家身份取自可信请求头 X-Merchant-Id，请求体在进入服务层前执行 Bean Validation。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @param dto 资质提交内容
   * @return 统一响应，数据为最新资质详情
   */
  @PostMapping("/qualification")
  public Result<MerchantQualificationVo> submitQualification(
      @RequestHeader("X-Merchant-Id") Long merchantId,
      @Valid @RequestBody QualificationSubmitDto dto) {
    return Result.success(merchantService.submitQualification(merchantId, dto));
  }

  /**
   * 查询当前商家的资质详情。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 统一响应，数据包含审核状态、脱敏字段和证照 URL
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  @GetMapping("/qualification")
  public Result<MerchantQualificationVo> getQualification(
      @RequestHeader("X-Merchant-Id") Long merchantId) {
    return Result.success(merchantService.getQualification(merchantId));
  }

  /**
   * 平台审核商家资质。
   *
   * 请求角色必须为 PLATFORM；驳回时必须在请求参数中提供非空原因。
   *
   * @param id 商家记录主键
   * @param approved true 表示通过，false 表示驳回
   * @param reason 驳回原因；通过时可为空
   * @param role 网关注入的可信用户角色
   * @return 统一成功响应，data 为 null
   * @throws BizException 非平台角色或驳回原因为空时抛出
   */
  @PostMapping("/{id}/audit")
  public Result<Void> audit(
      @PathVariable Long id,
      @RequestParam boolean approved,
      @RequestParam(required = false) String reason,
      @RequestHeader("X-User-Role") String role) {
    requirePlatform(role);
    if (!approved && (reason == null || reason.isBlank())) {
      throw new BizException(CommonCode.PARAM_INVALID, "PARAM_INVALID", "驳回时原因不能为空");
    }
    merchantService.audit(id, approved, reason);
    return Result.success(null);
  }

  /**
   * 查询当前商家的概要信息。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 统一响应，数据包含资质摘要和脱敏敏感字段
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  @GetMapping("/me")
  public Result<MerchantInfoVo> getMerchantInfo(@RequestHeader("X-Merchant-Id") Long merchantId) {
    return Result.success(merchantService.getMerchantInfo(merchantId));
  }

  /**
   * 查询当前商家的审核记录。
   *
   * 当前服务至多返回一条根据资质记录合成的审核记录。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 统一响应，数据为审核记录列表；无记录时为空列表
   */
  @GetMapping("/audit-log")
  public Result<List<MerchantAuditLogVo>> listAuditLog(
      @RequestHeader("X-Merchant-Id") Long merchantId) {
    return Result.success(merchantService.listAuditLog(merchantId));
  }

  /**
   * 校验调用角色是否为平台用户。
   *
   * @param role 网关注入的可信用户角色
   * @throws BizException 角色不是 PLATFORM 时抛出 FORBIDDEN
   */
  private void requirePlatform(String role) {
    if (!RoleEnum.PLATFORM.getCode().equals(role)) {
      throw new BizException(CommonCode.FORBIDDEN, "FORBIDDEN", "无权限访问");
    }
  }
}
