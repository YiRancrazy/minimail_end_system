package com.yirancrazy.minimall.merchant.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import com.yirancrazy.minimall.api.feign.IdFeignClient;
import com.yirancrazy.minimall.common.exception.BizException;
import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.common.result.Result;
import com.yirancrazy.minimall.common.util.CursorUtils;
import com.yirancrazy.minimall.merchant.constant.MerchantAuditStatusEnum;
import com.yirancrazy.minimall.merchant.constant.MerchantCodeEnum;
import com.yirancrazy.minimall.merchant.constant.WithdrawStatusEnum;
import com.yirancrazy.minimall.merchant.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.merchant.dto.WithdrawPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantPO;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;
import com.yirancrazy.minimall.merchant.manager.MerchantManager;
import com.yirancrazy.minimall.merchant.manager.MerchantWithdrawManager;
import com.yirancrazy.minimall.merchant.service.WithdrawService;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现领域服务实现，处理提现申请、平台审核与分页查询。
 * @Version: 1.1
 * @DateTime: 2026/08/16
 **/
@Slf4j
@Service
public class WithdrawServiceImpl implements WithdrawService {

    // 单笔/在途提现上限：1 亿元；在途提现与本次申请合计不得突破，防重复占额
    private static final BigDecimal MAX_WITHDRAW_AMOUNT = new BigDecimal("100000000");
    private static final String WITHDRAW_NO_PREFIX = "W";
    private static final String WITHDRAW_BIZ_TAG = "WITHDRAW";

    private final MerchantWithdrawManager withdrawManager;
    private final MerchantManager merchantManager;
    private final IdFeignClient idFeignClient;

    public WithdrawServiceImpl(MerchantWithdrawManager withdrawManager,
                               MerchantManager merchantManager,
                               IdFeignClient idFeignClient) {
        this.withdrawManager = withdrawManager;
        this.merchantManager = merchantManager;
        this.idFeignClient = idFeignClient;
    }

    /**
     * 商家发起提现申请：校验资质已审核通过、金额不超上限且与在途提现合计不超上限，生成提现单号并落库为 PENDING。
     * 可提现余额（已支付流水−退款−在途）依赖 pay 侧账户体系，暂未接入，待后续 Feign 打通后补充余额扣减。
     * @param merchantId 商家ID
     * @param dto 提现申请入参
     * @return 提现申请ID
     */
    @Override
    public Long apply(Long merchantId, WithdrawApplyDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().signum() <= 0) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_AMOUNT_INVALID);
        }
        if (dto.getAmount().compareTo(MAX_WITHDRAW_AMOUNT) > 0) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_AMOUNT_EXCEED_LIMIT);
        }
        requireAudited(merchantId);
        BigDecimal inFlight = withdrawManager.sumInFlightAmount(merchantId);
        if (inFlight.add(dto.getAmount()).compareTo(MAX_WITHDRAW_AMOUNT) > 0) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_AMOUNT_EXCEED_LIMIT);
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
     * 平台审核通过：条件更新（仅 PENDING→APPROVED）保证并发/重复审核只放行一次，0 行影响说明状态不允许审核。
     * @param id 提现申请ID
     */
    @Override
    public void approve(Long id) {
        // 先查存在性，区分"单不存在"与"状态不允许审核"
        getById(id);
        int rows = withdrawManager.updateStatusIf(id,
            WithdrawStatusEnum.PENDING.intCode(), WithdrawStatusEnum.APPROVED.intCode(), null, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_STATUS_INVALID);
        }
        log.info("withdraw approved, id={}", id);
    }

    /**
     * 平台审核驳回：条件更新（仅 PENDING→REJECTED）保证并发/重复审核只放行一次，0 行影响说明状态不允许审核。
     * @param id 提现申请ID
     * @param reason 驳回原因
     */
    @Override
    public void reject(Long id, String reason) {
        getById(id);
        int rows = withdrawManager.updateStatusIf(id,
            WithdrawStatusEnum.PENDING.intCode(), WithdrawStatusEnum.REJECTED.intCode(), reason, LocalDateTime.now());
        if (rows == 0) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_STATUS_INVALID);
        }
        log.info("withdraw rejected, id={}, reason={}", id, reason);
    }

    private MerchantWithdrawPO getById(Long id) {
        MerchantWithdrawPO po = withdrawManager.getById(id);
        if (po == null) {
            throw new BizException(MerchantCodeEnum.WITHDRAW_NOT_FOUND);
        }
        return po;
    }

    /**
     * 校验商家资质已审核通过，未过审不允许发起提现。
     * @param merchantId 商家ID（t_merch_merchant.user_id）
     */
    private void requireAudited(Long merchantId) {
        MerchantPO merchant = merchantManager.getOne(
            Wrappers.lambdaQuery(MerchantPO.class).eq(MerchantPO::getUserId, merchantId));
        if (merchant == null
            || !merchant.getAuditStatus().equals(Integer.parseInt(MerchantAuditStatusEnum.APPROVED.getCode()))) {
            throw new BizException(MerchantCodeEnum.MERCHANT_NOT_AUDITED);
        }
    }

    /**
     * 生成提现单号：优先取 Id 服务全局唯一 ID（业务标签 WITHDRAW），服务不可用/返回失败时回退时间戳+随机并 WARN。
     * @return 提现单号，保证非空
     */
    private String generateWithdrawNo() {
        try {
            Result<Long> idResult = idFeignClient.nextId(WITHDRAW_BIZ_TAG);
            if (idResult != null && "00000".equals(idResult.getCode()) && idResult.getData() != null) {
                return WITHDRAW_NO_PREFIX + idResult.getData();
            }
        }
        catch (Exception e) {
            log.warn("id service unavailable, fallback to timestamp-based withdraw no", e);
        }
        return WITHDRAW_NO_PREFIX + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }
}
