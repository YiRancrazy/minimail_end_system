package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDto;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDto;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVo;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVo;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVo;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质领域服务契约；定义资质提交、查询、平台审核、商家概览、审核记录以及内部管理分页和详情能力。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface MerchantService {

  /**
   * 按商家用户 ID 新建或刷新资质，并将审核状态重置为待审核。
   *
   * 已有记录会同步清空历史审核原因和审核时间；敏感字段由持久化层的加密类型处理器加密。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @param dto 已校验的资质材料
   * @return 包含脱敏字段和可访问证照 URL 的资质详情
   */
  MerchantQualificationVo submitQualification(Long merchantId, QualificationSubmitDto dto);

  /**
   * 查询指定商家的资质详情。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 包含脱敏字段和可访问证照 URL 的资质详情
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  MerchantQualificationVo getQualification(Long merchantId);

  /**
   * 审核待处理的商家资质，并将结果更新为通过或驳回。
   *
   * 驳回必须提供非空原因；仅待审核状态可被审核，更新冲突按重复审核处理。
   *
   * @param merchantId 商家记录主键，不是商家用户 ID
   * @param approved true 表示通过，false 表示驳回
   * @param reason 驳回原因；通过时可忽略
   * @throws BizException 驳回原因为空、资质不存在、状态非待审核或更新冲突时抛出
   */
  void audit(Long merchantId, boolean approved, String reason);

  /**
   * 查询商家端概览信息。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 商家名称、证照号、审核状态和脱敏敏感字段
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  MerchantInfoVo getMerchantInfo(Long merchantId);

  /**
   * 查询指定商家的审核记录。
   *
   * 当前实现根据商家记录构造至多一条资质审核记录；商家不存在时返回空列表。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 审核记录列表；无资质记录时为空列表
   */
  List<MerchantAuditLogVo> listAuditLog(Long merchantId);

  /**
   * 按 ID 倒序游标分页查询商家资质。
   *
   * 支持商家名称关键字和审核状态组合筛选，并返回下一页游标及是否还有更多数据。
   *
   * @param dto 游标、每页条数和筛选条件
   * @return 商家管理视图分页结果
   */
  CursorPageVo<MerchantManageVo> page(MerchantPageDto dto);

  /**
   * 按商家记录主键查询内部管理详情。
   *
   * @param merchantId 商家记录主键
   * @return 商家资质管理视图
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  MerchantManageVo detail(Long merchantId);
}
