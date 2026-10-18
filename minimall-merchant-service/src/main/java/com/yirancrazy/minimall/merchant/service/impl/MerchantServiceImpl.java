package com.yirancrazy.minimall.merchant.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.dto.merchant.MerchantManageVO;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CommonCode;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.common.util.MinioUtil;
import com.yirancrazy.minimall.common.util.SensitiveDataUtils;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum;
import com.yirancrazy.minimall.merchant.dto.MerchantPageDTO;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVO;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MerchantServiceImpl 服务实现，实现相关业务逻辑。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Slf4j
@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantManager merchantManager;
    private final MinioUtil minioUtil;

    /**
     * 构造 MerchantServiceImpl 实例。
     * @param merchantManager merchantManager 参数
     * @param minioUtil minioUtil 参数
     */
    public MerchantServiceImpl(MerchantManager merchantManager, MinioUtil minioUtil) {
        this.merchantManager = merchantManager;
        this.minioUtil = minioUtil;
    }


    /**
     * 执行 submitQualification 方法。
     * @param merchantId merchantId 参数
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public MerchantQualificationVO submitQualification(Long merchantId, QualificationSubmitDTO dto) {
        MerchantPO po = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (po == null) {
            po = new MerchantPO();
            po.setUserId(merchantId);
            po.setMerchantName(dto.getMerchantName());
            po.setLicenseNo(dto.getLicenseNo());
            po.setLicenseImageUrl(dto.getLicenseImageUrl());
            applySensitiveFields(po, dto);
            po.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode()));
            merchantManager.save(po);
        }
        else {
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
        return toVO(po);
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public MerchantQualificationVO getQualification(Long merchantId) {
        return toVO(getByUserId(merchantId));
    }


    /**
     * 执行 audit 方法。
     * @param merchantId merchantId 参数
     * @param approved approved 参数
     * @param reason reason 参数
     */
    @Override
    public void audit(Long merchantId, boolean approved, String reason) {
        if (!approved && (reason == null || reason.isBlank())) {
            throw new BizException(CommonCode.PARAM_INVALID, "PARAM_INVALID", "驳回时原因不能为空");
        }
        MerchantPO po = merchantManager.getById(merchantId);
        if (po == null) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
        }
        // 非待审核状态（含脏数据 null）一律禁止审核，避免重复审核与并发场景下的状态覆盖
        Integer current = po.getAuditStatus();
        if (current == null || current != Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode())) {
            throw new BizException(MerchantCodeEnum.MERCHANT_ALREADY_AUDITED);
        }
        po.setAuditStatus(Integer.parseInt(
            approved ? MerchantAuditStatusEnum.APPROVED.getCode() : MerchantAuditStatusEnum.REJECTED.getCode()));
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
     * 获取根据用户ID。
     * @param merchantId 商户 ID
     * @return 查询结果
     * @throws BizException 业务校验失败时抛出
     */
    private MerchantPO getByUserId(Long merchantId) {
        MerchantPO po = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (po == null) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
        }
        return po;
    }


    /**
     * 获取相关数据。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public MerchantInfoVO getMerchantInfo(Long merchantId) {
        MerchantPO po = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (po == null) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
        }
        return MerchantInfoVO.from(po);
    }


    /**
     * 查询列表数据。
     * @param merchantId merchantId 参数
     * @return 结果列表
     */
    @Override
    public List<MerchantAuditLogVO> listAuditLog(Long merchantId) {
        MerchantPO po = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (po == null) {
            return Collections.emptyList();
        }
        MerchantAuditLogVO vo = new MerchantAuditLogVO();
        vo.setId(po.getId());
        vo.setAuditType("QUALIFICATION");
        vo.setTargetId(po.getUserId());
        vo.setDecision(po.getAuditStatus());
        vo.setReason(po.getAuditReason());
        vo.setAuditAt(po.getAuditAt());
        return Collections.singletonList(vo);
    }


    /**
     * 执行SensitiveFields逻辑。
     * @param po 持久化对象
     * @param dto 请求参数
     */
    private void applySensitiveFields(MerchantPO po, QualificationSubmitDTO dto) {
        po.setLegalPersonEnc(dto.getLegalPerson());
        po.setLegalPhoneEnc(dto.getLegalPhone());
        po.setIdCardNoEnc(dto.getIdCardNo());
        po.setBusinessLicenseNoEnc(dto.getBusinessLicenseNo());
        po.setBankAccountEnc(dto.getBankAccount());
    }

    /**
     * 转换或解析VO。
     * @param po 持久化对象
     * @return 视图对象
     */
    private MerchantQualificationVO toVO(MerchantPO po) {
        MerchantQualificationVO vo = new MerchantQualificationVO();
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
     * 分页查询数据。
     * @param dto dto 参数
     * @return 处理结果
     */
    @Override
    public CursorPageVO<MerchantManageVO> page(MerchantPageDTO dto) {
        int limit = dto.getLimit();
        Long lastId = CursorUtils.decode(dto.getCursor());
        LambdaQueryWrapper<MerchantPO> wrapper = Wrappers.lambdaQuery(MerchantPO.class);
        wrapper.lt(lastId != null, MerchantPO::getId, lastId);
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            wrapper.like(MerchantPO::getMerchantName, dto.getKeyword());
        }
        if (dto.getAuditStatus() != null) {
            wrapper.eq(MerchantPO::getAuditStatus, dto.getAuditStatus());
        }
        wrapper.orderByDesc(MerchantPO::getId);
        wrapper.last("LIMIT " + (limit + 1));
        List<MerchantPO> records = merchantManager.list(wrapper);
        return CursorPageVO.of(records, limit, MerchantPO::getId).map(this::toManageVO);
    }


    /**
     * 执行 detail 方法。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Override
    public MerchantManageVO detail(Long merchantId) {
        MerchantPO po = merchantManager.getById(merchantId);
        if (po == null) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
        }
        return toManageVO(po);
    }


    /**
     * 转换或解析ManageVO。
     * @param po 持久化对象
     * @return 视图对象
     */
    private MerchantManageVO toManageVO(MerchantPO po) {
        return new MerchantManageVO(po.getId(), po.getUserId(), po.getMerchantName(),
            po.getLicenseNo(), po.getLicenseImageUrl(), po.getAuditStatus(), po.getAuditReason(),
            po.getAuditAt(), po.getCreateTime());
    }
}
