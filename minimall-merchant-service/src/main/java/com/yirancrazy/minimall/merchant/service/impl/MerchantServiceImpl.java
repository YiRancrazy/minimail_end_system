package com.yirancrazy.minimall.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVo;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVo;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDto;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDto;
import com.yirancrazy.minimall.merchant.entity.MerchantPo;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVo;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVo;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVo;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质服务实现；依赖 MerchantManager 持久化资质、MinioUtil 解析证照访问地址，并执行状态流转、敏感字段脱敏、游标分页和业务异常映射。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Slf4j
@Service
public class MerchantServiceImpl implements MerchantService {

  private final MerchantManager merchantManager;
  private final MinioUtil minioUtil;

  /**
   * 注入商家资质持久化与证照 URL 解析依赖。
   *
   * @param merchantManager 商家资质 Manager
   * @param minioUtil 对象存储公开 URL 解析工具
   */
  public MerchantServiceImpl(MerchantManager merchantManager, MinioUtil minioUtil) {
    this.merchantManager = merchantManager;
    this.minioUtil = minioUtil;
  }

  /**
   * 按商家用户 ID 新建或刷新资质，并重置为待审核状态。
   *
   * 首次提交创建记录；再次提交更新主体和证照信息，同时清空历史审核原因与审核时间。敏感字段先写入 Enc 字段，由 MyBatis 类型处理器在落库时加密。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @param dto 已校验的资质材料
   * @return 包含脱敏字段和可访问证照 URL 的资质详情
   */
  @Override
  public MerchantQualificationVo submitQualification(Long merchantId, QualificationSubmitDto dto) {
    MerchantPo po =
        merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPo.class).eq(MerchantPo::getUserId, merchantId));
    if (po == null) {
      po = new MerchantPo();
      po.setUserId(merchantId);
      po.setMerchantName(dto.getMerchantName());
      po.setLicenseNo(dto.getLicenseNo());
      po.setLicenseImageUrl(dto.getLicenseImageUrl());
      applySensitiveFields(po, dto);
      po.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode()));
      merchantManager.save(po);
    } else {
      po.setMerchantName(dto.getMerchantName());
      po.setLicenseNo(dto.getLicenseNo());
      po.setLicenseImageUrl(dto.getLicenseImageUrl());
      applySensitiveFields(po, dto);
      po.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode()));
      po.setAuditReason(null);
      po.setAuditAt(null);
      merchantManager.updateById(po);
    }
    log.info("资质已提交，merchantId={}，merchantName={}", merchantId, dto.getMerchantName());
    return toVo(po);
  }

  /**
   * 查询指定商家的资质详情。
   *
   * @param merchantId 可信商家身份，对应商家表的 userId
   * @return 包含脱敏字段和可访问证照 URL 的资质详情
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  @Override
  public MerchantQualificationVo getQualification(Long merchantId) {
    return toVo(getByUserId(merchantId));
  }

  /**
   * 审核待处理的商家资质并持久化通过或驳回结果。
   *
   * 驳回先校验原因非空；仅待审核状态可处理。更新时依赖 BasePo 的乐观锁版本，updateById 返回 false 视为并发冲突并抛出重复审核异常。
   *
   * @param merchantId 商家记录主键，不是商家用户 ID
   * @param approved true 表示通过，false 表示驳回
   * @param reason 驳回原因；通过时忽略并清空历史原因
   * @throws BizException 驳回原因为空、资质不存在、状态非待审核或更新冲突时抛出
   */
  @Override
  public void audit(Long merchantId, boolean approved, String reason) {
    if (!approved && (reason == null || reason.isBlank())) {
      throw new BizException(CommonCode.PARAM_INVALID, "PARAM_INVALID", "驳回时原因不能为空");
    }
    MerchantPo po = merchantManager.getById(merchantId);
    if (po == null) {
      throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
    }
    // 非待审核状态（含脏数据 null）一律禁止审核，避免重复审核与并发场景下的状态覆盖
    Integer current = po.getAuditStatus();
    if (current == null || current != Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode())) {
      throw new BizException(MerchantCodeEnum.MERCHANT_ALREADY_AUDITED);
    }
    po.setAuditStatus(
        Integer.parseInt(
            approved
                ? MerchantAuditStatusEnum.APPROVED.getCode()
                : MerchantAuditStatusEnum.REJECTED.getCode()));
    // 通过时清理历史驳回原因，驳回时记录本次原因
    po.setAuditReason(approved ? null : reason);
    po.setAuditAt(LocalDateTime.now());
    // 并发审核时第二个事务受乐观锁版本冲突影响受影响行数为 0，updateById 返回 false，必须显式失败而非静默成功
    if (!merchantManager.updateById(po)) {
      log.warn("商家审核冲突，merchantId={}，approved={}", merchantId, approved);
      throw new BizException(MerchantCodeEnum.MERCHANT_ALREADY_AUDITED);
    }
    log.info("商家审核完成，merchantId={}，approved={}", merchantId, approved);
  }

  /**
   * 按商家用户 ID 查询资质，不存在时转换为领域异常。
   *
   * @param merchantId 商家用户 ID，对应表字段 user_id
   * @return 商家资质持久化对象
   * @throws BizException 未找到资质时抛出 MERCHANT_NOT_FOUND
   */
  private MerchantPo getByUserId(Long merchantId) {
    MerchantPo po =
        merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPo.class).eq(MerchantPo::getUserId, merchantId));
    if (po == null) {
      throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
    }
    return po;
  }

  /**
   * 按商家用户 ID 查询并构建脱敏概览。
   *
   * @param merchantId 商家用户 ID，对应表字段 user_id
   * @return 商家概览视图
   * @throws BizException 未找到资质时抛出 MERCHANT_NOT_FOUND
   */
  @Override
  public MerchantInfoVo getMerchantInfo(Long merchantId) {
    MerchantPo po =
        merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPo.class).eq(MerchantPo::getUserId, merchantId));
    if (po == null) {
      throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
    }
    return MerchantInfoVo.from(po);
  }

  /**
   * 根据商家资质记录构造审核记录列表。
   *
   * 资质不存在时返回空列表；存在时返回一条 QUALIFICATION 记录，不填充 auditorId。
   *
   * @param merchantId 商家用户 ID，对应表字段 user_id
   * @return 至多一条资质审核记录
   */
  @Override
  public List<MerchantAuditLogVo> listAuditLog(Long merchantId) {
    MerchantPo po =
        merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPo.class).eq(MerchantPo::getUserId, merchantId));
    if (po == null) {
      return Collections.emptyList();
    }
    MerchantAuditLogVo vo = new MerchantAuditLogVo();
    vo.setId(po.getId());
    vo.setAuditType("QUALIFICATION");
    vo.setTargetId(po.getUserId());
    vo.setDecision(po.getAuditStatus());
    vo.setReason(po.getAuditReason());
    vo.setAuditAt(po.getAuditAt());
    return Collections.singletonList(vo);
  }

  /**
   * 将请求中的敏感字段写入商家持久化对象的加密字段。
   *
   * 本方法只赋值，AES-GCM 加密由 EncryptedStringTypeHandler 在数据库写入阶段执行。
   *
   * @param po 待保存的商家持久化对象
   * @param dto 资质提交内容
   */
  private void applySensitiveFields(MerchantPo po, QualificationSubmitDto dto) {
    po.setLegalPersonEnc(dto.getLegalPerson());
    po.setLegalPhoneEnc(dto.getLegalPhone());
    po.setIdCardNoEnc(dto.getIdCardNo());
    po.setBusinessLicenseNoEnc(dto.getBusinessLicenseNo());
    po.setBankAccountEnc(dto.getBankAccount());
  }

  /**
   * 将商家持久化对象转换为资质详情视图。
   *
   * 证照对象键通过 MinioUtil 解析为公开 URL，手机号、身份证和银行卡执行脱敏；merchantId 与 userId 均映射自相同用户 ID。
   *
   * @param po 商家持久化对象
   * @return 资质详情视图
   */
  private MerchantQualificationVo toVo(MerchantPo po) {
    MerchantQualificationVo vo = new MerchantQualificationVo();
    vo.setId(po.getId());
    // merchantId 与 userId 同义，对外暴露稳定字段名
    vo.setMerchantId(po.getUserId());
    vo.setUserId(po.getUserId());
    vo.setMerchantName(po.getMerchantName());
    vo.setLicenseNo(po.getLicenseNo());
    vo.setLicenseImageUrl(minioUtil.resolvePublicUrl(po.getLicenseImageUrl()));
    vo.setAuditStatus(po.getAuditStatus());
    vo.setAuditReason(po.getAuditReason());
    vo.setAuditAt(po.getAuditAt());
    vo.setLegalPhoneMasked(SensitiveDataUtils.maskPhone(po.getLegalPhoneEnc()));
    vo.setIdCardNoMasked(SensitiveDataUtils.maskIdCard(po.getIdCardNoEnc()));
    vo.setBankAccountMasked(SensitiveDataUtils.maskBankCard(po.getBankAccountEnc()));
    return vo;
  }

  /**
   * 按 ID 倒序执行商家资质游标分页。
   *
   * 游标解码后按 ID 小于游标筛选，支持商家名称模糊匹配和审核状态精确匹配；额外查询一条记录用于判断 hasMore。
   *
   * @param dto 游标、每页条数和筛选条件
   * @return 已转换为管理视图的分页结果
   */
  @Override
  public CursorPageVo<MerchantManageVo> page(MerchantPageDto dto) {
    final int limit = dto.getLimit();
    Long lastId = CursorUtils.decode(dto.getCursor());
    LambdaQueryWrapper<MerchantPo> wrapper = Wrappers.lambdaQuery(MerchantPo.class);
    wrapper.lt(lastId != null, MerchantPo::getId, lastId);
    if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
      wrapper.like(MerchantPo::getMerchantName, dto.getKeyword());
    }
    if (dto.getAuditStatus() != null) {
      wrapper.eq(MerchantPo::getAuditStatus, dto.getAuditStatus());
    }
    wrapper.orderByDesc(MerchantPo::getId);
    wrapper.last("LIMIT " + (limit + 1));
    List<MerchantPo> records = merchantManager.list(wrapper);
    return CursorPageVo.of(records, limit, MerchantPo::getId).map(this::toManageVo);
  }

  /**
   * 按商家记录主键查询内部管理详情。
   *
   * @param merchantId 商家记录主键
   * @return 商家资质管理视图
   * @throws BizException 商家资质不存在时抛出 MERCHANT_NOT_FOUND
   */
  @Override
  public MerchantManageVo detail(Long merchantId) {
    MerchantPo po = merchantManager.getById(merchantId);
    if (po == null) {
      throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
    }
    return toManageVo(po);
  }

  /**
   * 将商家持久化对象转换为内部管理视图。
   *
   * 该转换直接透出证照对象键和审核字段，不执行敏感字段脱敏或 URL 解析。
   *
   * @param po 商家持久化对象
   * @return 商家资质管理视图
   */
  private MerchantManageVo toManageVo(MerchantPo po) {
    return new MerchantManageVo(
        po.getId(),
        po.getUserId(),
        po.getMerchantName(),
        po.getLicenseNo(),
        po.getLicenseImageUrl(),
        po.getAuditStatus(),
        po.getAuditReason(),
        po.getAuditAt(),
        po.getCreateTime());
  }
}
