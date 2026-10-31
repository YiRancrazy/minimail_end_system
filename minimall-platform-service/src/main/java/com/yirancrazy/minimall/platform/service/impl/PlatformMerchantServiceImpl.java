package com.yirancrazy.minimall.platform.service.impl;

import com.yirancrazy.minimall.api.dto.common.InternalPageQuery;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.platform.constant.PlatformCodeEnum;
import com.yirancrazy.minimall.platform.dto.MerchantQualificationAuditDto;
import com.yirancrazy.minimall.platform.dto.PlatformMerchantPageDto;
import com.yirancrazy.minimall.platform.gateway.PlatformRemoteGateway;
import com.yirancrazy.minimall.platform.service.PlatformMerchantService;
import com.yirancrazy.minimall.platform.vo.MerchantQualificationVo;
import com.yirancrazy.minimall.platform.vo.PlatformMerchantVo;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 平台商家管理实现，通过远程商家服务查询数据，并补充 MinIO 公开访问地址.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class PlatformMerchantServiceImpl implements PlatformMerchantService {

  private final PlatformRemoteGateway platformRemoteGateway;
  private final MinioUtil minioUtil;

  /**
   * 注入远程商家访问网关和 MinIO 地址解析工具.
   *
   * @param platformRemoteGateway 平台远程服务访问网关
   * @param minioUtil MinIO 公开地址解析工具
   */
  public PlatformMerchantServiceImpl(
      PlatformRemoteGateway platformRemoteGateway, MinioUtil minioUtil) {
    this.platformRemoteGateway = platformRemoteGateway;
    this.minioUtil = minioUtil;
  }

  /**
   * 将商家分页条件转换为内部查询，并映射商家服务返回结果.
   *
   * <p>远程响应或数据为空时返回空分页；未指定 limit 时使用 20，远程失败状态未单独转换为异常。
   *
   * @param dto 商家分页筛选条件
   * @return 商家视图分页
   */
  @Override
  public CursorPageVo<PlatformMerchantVo> page(PlatformMerchantPageDto dto) {
    InternalPageQuery q = new InternalPageQuery();
    q.setCursor(dto.getCursor());
    q.setLimit(dto.getLimit());
    q.setKeyword(dto.getKeyword());
    q.setStatus(dto.getAuditStatus());
    Result<CursorPageVo<MerchantManageVo>> r = platformRemoteGateway.pageMerchants(q);
    if (r == null || r.getData() == null) {
      log.warn("商家分页查询返回空结果");
      return CursorPageVo.of(
          List.of(), dto.getLimit() == null ? 20 : dto.getLimit(), v -> v.getMerchantId());
    }
    return r.getData().map(this::toPlatformVo);
  }

  /**
   * 查询商家详情并转换为平台管理视图.
   *
   * @param merchantId 商家 ID
   * @return 商家管理视图
   * @throws BizException 商家服务响应为空或商家不存在时抛出
   */
  @Override
  public PlatformMerchantVo detail(Long merchantId) {
    Result<MerchantManageVo> r = platformRemoteGateway.merchantDetail(merchantId);
    if (r == null || r.getData() == null) {
      throw new BizException(PlatformCodeEnum.PLATFORM_MERCHANT_NOT_FOUND);
    }
    return toPlatformVo(r.getData());
  }

  /**
   * 以审核状态 0 查询待审核商家资质，并附加证照图片访问地址.
   *
   * <p>商家服务返回失败、空响应或空数据时记录警告并抛出系统错误，不返回空分页。
   *
   * @param cursor 上一页游标，为空时从首条开始
   * @param limit 每页数量
   * @return 待审核资质视图分页
   * @throws BizException 商家服务不可用或返回异常响应时抛出
   */
  @Override
  public CursorPageVo<MerchantQualificationVo> listPendingQualifications(String cursor, int limit) {
    InternalPageQuery query = new InternalPageQuery();
    query.setCursor(cursor);
    query.setLimit(limit);
    // 商家资质待审核码：MerchantAuditStatusEnum.PENDING.code = 0
    query.setStatus(0);
    Result<CursorPageVo<MerchantManageVo>> result = platformRemoteGateway.pageMerchants(query);
    if (result == null
        || !CommonCode.SUCCESS.equals(result.getCode())
        || result.getData() == null) {
      log.warn("查询待审核商家资质失败，cursor={}，limit={}", cursor, limit);
      throw new BizException(CommonCode.SYS_ERROR, "商家服务处理失败");
    }
    return result.getData().map(this::toQualificationVo);
  }

  /**
   * 记录资质审核参数并提交商家服务，要求远程响应为成功状态.
   *
   * <p>该方法产生远程审核副作用，不写入本地数据库。
   *
   * @param merchantId 商家 ID
   * @param dto 资质 ID、审核结果及说明
   * @throws BizException 商家服务响应为空或状态非成功时抛出
   */
  public void auditQualification(Long merchantId, MerchantQualificationAuditDto dto) {
    log.info(
        "平台审核商家资质，merchantId={}，qualificationId={}，approved={}，reason={}",
        merchantId,
        dto.getQualificationId(),
        dto.getApproved(),
        dto.getReason());
    Result<Void> result =
        platformRemoteGateway.auditMerchant(merchantId, dto.getApproved(), dto.getReason());
    if (result == null || !CommonCode.SUCCESS.equals(result.getCode())) {
      log.warn("merchant-service 拒绝商家审核，merchantId={}，result={}", merchantId, result);
      throw new BizException(CommonCode.SYS_ERROR, "商家服务处理失败");
    }
  }

  /**
   * 将商家服务的字段逐项复制为平台商家视图.
   *
   * @param v 商家服务返回的管理数据
   * @return 平台商家视图
   */
  private PlatformMerchantVo toPlatformVo(MerchantManageVo v) {
    return new PlatformMerchantVo(
        v.getMerchantId(),
        v.getUserId(),
        v.getMerchantName(),
        v.getLicenseNo(),
        v.getAuditStatus(),
        v.getAuditReason(),
        v.getAuditAt(),
        v.getCreateTime());
  }

  /**
   * 将商家数据转换为资质审核视图，并解析证照图片的公开访问地址.
   *
   * <p>资质主键复用商家 ID；审核状态码转换为可读状态名，提交和审核时间分别取创建与审核时间。
   *
   * @param manage 商家服务返回的管理数据
   * @return 资质审核视图
   */
  private MerchantQualificationVo toQualificationVo(MerchantManageVo manage) {
    MerchantQualificationVo vo = new MerchantQualificationVo();
    vo.setId(manage.getMerchantId());
    vo.setMerchantId(manage.getMerchantId());
    vo.setMerchantName(manage.getMerchantName());
    vo.setLicenseNo(manage.getLicenseNo());
    vo.setLicenseImageUrl(minioUtil.resolvePublicUrl(manage.getLicenseImageUrl()));
    vo.setStatus(auditStatusName(manage.getAuditStatus()));
    vo.setRejectReason(manage.getAuditReason());
    vo.setSubmitTime(manage.getCreateTime());
    vo.setAuditTime(manage.getAuditAt());
    return vo;
  }

  /**
   * 将商家审核状态码转换为平台展示状态名.
   *
   * <p>0、1、2 分别映射 PENDING、APPROVED、REJECTED；null 按 PENDING 处理，未知码原样转字符串.
   *
   * @param code 商家审核状态码
   * @return 展示状态名或未知状态码文本
   */
  private String auditStatusName(Integer code) {
    if (code == null) {
      return "PENDING";
    }
    switch (code) {
      case 0:
        return "PENDING";
      case 1:
        return "APPROVED";
      case 2:
        return "REJECTED";
      default:
        return String.valueOf(code);
    }
  }
}
