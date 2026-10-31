package com.yirancrazy.minimall.platform.service;

import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDto;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDto;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVo;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVo;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台端商家管理与资质审核业务契约，隔离接口层和远程商家服务.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface PlatformMerchantService {

  /**
   * 按游标、关键词和审核状态查询商家管理列表.
   *
   * @param dto 商家分页筛选条件
   * @return 商家视图分页；远程结果为空时返回空页
   */
  CursorPageVo<PlatformMerchantVo> page(PlatformMerchantPageDto dto);

  /**
   * 查询指定商家的管理详情.
   *
   * @param merchantId 商家 ID
   * @return 商家管理视图
   * @throws com.yirancrazy.minimall.common.exception.BizException 商家服务未返回商家数据时抛出
   */
  PlatformMerchantVo detail(Long merchantId);

  /**
   * 查询待审核商家资质，分页状态由实现固定为待审核.
   *
   * @param cursor 上一页游标，为空时从首条开始
   * @param limit 每页数量
   * @return 待审核资质视图分页
   * @throws com.yirancrazy.minimall.common.exception.BizException 商家服务返回失败或空数据时抛出
   */
  CursorPageVo<MerchantQualificationVo> listPendingQualifications(String cursor, int limit);

  /**
   * 提交商家资质审核结果并触发商家服务审核状态变更.
   *
   * @param merchantId 商家 ID
   * @param dto 资质 ID、审核结果及说明
   * @throws com.yirancrazy.minimall.common.exception.BizException 商家服务拒绝或不可用时抛出
   */
  void auditQualification(Long merchantId, MerchantQualificationAuditDto dto);
}
