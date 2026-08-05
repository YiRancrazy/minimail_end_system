package com.yirancrazy.minimall.merchant.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum;
import com.yirancrazy.minimall.merchant.constant.WithdrawStatusEnum;
import com.yirancrazy.minimall.merchant.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.merchant.dto.WithdrawPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.merchant.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.merchant.service.WithdrawService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现领域服务实现，处理提现申请、平台审核与分页查询。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Slf4j
@Service
public class WithdrawServiceImpl implements WithdrawService {

    private final MerchantWithdrawManager withdrawManager;

    public WithdrawServiceImpl(MerchantWithdrawManager withdrawManager) {
        this.withdrawManager = withdrawManager;
    }

    /**
     * 商家发起提现申请，生成提现单号并落库为 PENDING 状态。
     * @param merchantId 商家ID
     * @param dto 提现申请入参
     * @return 提现申请ID
     */
    @Override
    public Long apply(Long merchantId, WithdrawApplyDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_AMOUNT_INVALID);
        }
        MerchantWithdrawPO po = new MerchantWithdrawPO();
        po.setMerchantId(merchantId);
        po.setWithdrawNo(generateWithdrawNo());
        po.setAmount(dto.getAmount());
        po.setStatus(WithdrawStatusEnum.PENDING.intCode());
        po.setAppliedAt(LocalDateTime.now());
        withdrawManager.save(po);
        log.info("withdraw applied, id={}, merchantId={}, amount={}", po.getId(), merchantId, dto.getAmount());
        return po.getId();
    }

    /**
     * 商家分页查询自身提现申请，强制绑定 merchantId，按 ID 倒序游标分页。
     * @param dto 分页入参
     * @return 提现申请分页结果
     */
    @Override
    public CursorPageVO<MerchantWithdrawPO> pageByMerchant(WithdrawPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<MerchantWithdrawPO> records = withdrawManager.list(Wrappers.lambdaQuery(MerchantWithdrawPO.class)
            .eq(MerchantWithdrawPO::getMerchantId, dto.getMerchantId())
            .eq(dto.getStatus() != null, MerchantWithdrawPO::getStatus, dto.getStatus())
            .lt(lastId != null, MerchantWithdrawPO::getId, lastId)
            .orderByDesc(MerchantWithdrawPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, MerchantWithdrawPO::getId);
    }

    /**
     * 平台分页查询全部提现申请，支持按商家ID与状态过滤，按 ID 倒序游标分页。
     * @param dto 分页入参
     * @return 提现申请分页结果
     */
    @Override
    public CursorPageVO<MerchantWithdrawPO> pageAll(WithdrawPageDTO dto) {
        Long lastId = CursorUtils.decode(dto.getCursor());
        int limit = dto.getLimit();
        List<MerchantWithdrawPO> records = withdrawManager.list(Wrappers.lambdaQuery(MerchantWithdrawPO.class)
            .eq(dto.getMerchantId() != null, MerchantWithdrawPO::getMerchantId, dto.getMerchantId())
            .eq(dto.getStatus() != null, MerchantWithdrawPO::getStatus, dto.getStatus())
            .lt(lastId != null, MerchantWithdrawPO::getId, lastId)
            .orderByDesc(MerchantWithdrawPO::getId)
            .last("LIMIT " + (limit + 1)));
        return CursorPageVO.of(records, limit, MerchantWithdrawPO::getId);
    }

    /**
     * 平台审核通过，仅 PENDING 可审核通过，置为 APPROVED。
     * @param id 提现申请ID
     */
    @Override
    public void approve(Long id) {
        MerchantWithdrawPO po = getById(id);
        if (WithdrawStatusEnum.fromCode(po.getStatus()) != WithdrawStatusEnum.PENDING) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_STATUS_INVALID);
        }
        po.setStatus(WithdrawStatusEnum.APPROVED.intCode());
        po.setReviewedAt(LocalDateTime.now());
        withdrawManager.updateById(po);
        log.info("withdraw approved, id={}", id);
    }

    /**
     * 平台审核驳回，仅 PENDING 可驳回，置为 REJECTED 并记录原因。
     * @param id 提现申请ID
     * @param reason 驳回原因
     */
    @Override
    public void reject(Long id, String reason) {
        MerchantWithdrawPO po = getById(id);
        if (WithdrawStatusEnum.fromCode(po.getStatus()) != WithdrawStatusEnum.PENDING) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_STATUS_INVALID);
        }
        po.setStatus(WithdrawStatusEnum.REJECTED.intCode());
        po.setReason(reason);
        po.setReviewedAt(LocalDateTime.now());
        withdrawManager.updateById(po);
        log.info("withdraw rejected, id={}, reason={}", id, reason);
    }

    private MerchantWithdrawPO getById(Long id) {
        MerchantWithdrawPO po = withdrawManager.getById(id);
        if (po == null) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_NOT_FOUND);
        }
        return po;
    }

    private String generateWithdrawNo() {
        return "W" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}
