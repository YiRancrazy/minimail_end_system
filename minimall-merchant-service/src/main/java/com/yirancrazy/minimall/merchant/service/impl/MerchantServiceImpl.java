package com.yirancrazy.minimall.merchant.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum;
import com.yirancrazy.minimall.merchant.dto.QualificationSubmitDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.service.MerchantService;
import com.yirancrazy.minimall.merchant.vo.MerchantAuditLogVO;
import com.yirancrazy.minimall.merchant.vo.MerchantInfoVO;
import com.yirancrazy.minimall.merchant.vo.MerchantQualificationVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家资质服务实现，处理资质提交、查询与平台审核状态流转。
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Slf4j
@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantManager merchantManager;

    public MerchantServiceImpl(MerchantManager merchantManager) {
        this.merchantManager = merchantManager;
    }

    /**
     * 商家提交或更新资质，审核状态置为 PENDING。
     * @param merchantId 商家ID
     * @param dto 资质提交入参
     * @return 资质VO
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
            po.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode()));
            merchantManager.save(po);
        }
        else {
            po.setMerchantName(dto.getMerchantName());
            po.setLicenseNo(dto.getLicenseNo());
            po.setAuditStatus(Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode()));
            po.setAuditReason(null);
            po.setAuditAt(null);
            merchantManager.updateById(po);
        }
        log.info("qualification submitted, merchantId={}, merchantName={}", merchantId, dto.getMerchantName());
        return toVO(po);
    }

    /**
     * 查询商家资质，不存在时抛出 MERCHANT_NOT_FOUND。
     * @param merchantId 商家ID
     * @return 资质VO
     */
    @Override
    public MerchantQualificationVO getQualification(Long merchantId) {
        return toVO(getByUserId(merchantId));
    }

    /**
     * 平台审核商家资质，仅允许 PENDING 状态审核；approved=true 置 APPROVED，false 置 REJECTED 并记录原因。
     * @param merchantId 商家主体ID
     * @param approved 是否通过
     * @param reason 驳回原因
     */
    @Override
    public void audit(Long merchantId, boolean approved, String reason) {
        MerchantPO po = merchantManager.getById(merchantId);
        if (po == null) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
        }
        if (!po.getAuditStatus().equals(Integer.parseInt(MerchantAuditStatusEnum.PENDING.getCode()))) {
            throw new BizException(MerchantCodeEnum.MERCHANT_ALREADY_AUDITED);
        }
        po.setAuditStatus(Integer.parseInt(
            approved ? MerchantAuditStatusEnum.APPROVED.getCode() : MerchantAuditStatusEnum.REJECTED.getCode()));
        if (!approved) {
            po.setAuditReason(reason);
        }
        po.setAuditAt(LocalDateTime.now());
        merchantManager.updateById(po);
        log.info("merchant audited, merchantId={}, approved={}", merchantId, approved);
    }

    private MerchantPO getByUserId(Long merchantId) {
        MerchantPO po = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (po == null) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_FOUND);
        }
        return po;
    }

    /**
     * 商家获取自身信息，不存在时抛出 MERCHANT_NOT_FOUND。
     * @param merchantId 商家ID
     * @return 商家信息VO
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
     * 商家查看审核记录，返回资质审核记录。
     * @param merchantId 商家ID
     * @return 审核记录列表
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

    private MerchantQualificationVO toVO(MerchantPO po) {
        return new MerchantQualificationVO(
            po.getId(), po.getUserId(), po.getMerchantName(), po.getLicenseNo(),
            po.getAuditStatus(), po.getAuditReason(), po.getAuditAt());
    }
}
