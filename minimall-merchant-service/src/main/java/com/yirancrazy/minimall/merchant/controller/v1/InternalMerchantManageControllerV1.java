package com.yirancrazy.minimall.merchant.controller.v1;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDto;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台内部商家管理接口；将游标分页、详情和审核请求转换为 MerchantService 调用，路径限定在 /internal/merchant/manage，本类不实现鉴权。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@RestController
@RequestMapping("/internal/merchant/manage")
public class InternalMerchantManageControllerV1 {

  private final MerchantService merchantService;

  /**
   * 注入商家资质服务。
   *
   * @param merchantService 商家资质领域服务
   */
  public InternalMerchantManageControllerV1(MerchantService merchantService) {
    this.merchantService = merchantService;
  }

  /**
   * 将内部通用分页参数转换为商家筛选条件并查询。
   *
   * limit 为空时默认 20；query.status 映射为 auditStatus，关键字用于商家名称模糊筛选。
   *
   * @param query 游标、条数、关键字和审核状态
   * @return 商家管理视图游标分页结果
   */
  @GetMapping
  public Result<CursorPageVo<MerchantManageVo>> page(InternalPageQuery query) {
    MerchantPageDto dto = new MerchantPageDto();
    dto.setCursor(query.getCursor());
    dto.setLimit(query.getLimit() == null ? 20 : query.getLimit());
    dto.setKeyword(query.getKeyword());
    dto.setAuditStatus(query.getStatus());
    return Result.success(merchantService.page(dto));
  }

  /**
   * 按商家记录主键查询内部管理详情。
   *
   * @param merchantId 商家记录主键
   * @return 统一响应，数据为商家资质管理视图
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  @GetMapping("/{merchantId}")
  public Result<MerchantManageVo> detail(@PathVariable("merchantId") Long merchantId) {
    return Result.success(merchantService.detail(merchantId));
  }

  /**
   * 将平台内部审核请求委托给商家资质服务。
   *
   * 驳回原因是否必填及重复审核状态由服务层校验。
   *
   * @param merchantId 商家记录主键
   * @param approved true 表示通过，false 表示驳回
   * @param reason 驳回原因；通过时可为空
   * @return 统一成功响应，data 为 null
   * @throws BizException 资质不存在、状态非待审核、驳回原因为空或更新冲突时抛出
   */
  @PostMapping("/{merchantId}/audit")
  public Result<Void> audit(
      @PathVariable("merchantId") Long merchantId,
      @RequestParam boolean approved,
      @RequestParam(required = false) String reason) {
    merchantService.audit(merchantId, approved, reason);
    return Result.success(null);
  }
}
