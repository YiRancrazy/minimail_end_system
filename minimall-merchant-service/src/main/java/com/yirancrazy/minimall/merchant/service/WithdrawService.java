package com.yirancrazy.minimall.merchant.service;

import com.yirancrazy.minimall.common.result.CursorPageVO;
import com.yirancrazy.minimall.merchant.dto.WithdrawApplyDTO;
import com.yirancrazy.minimall.merchant.dto.WithdrawPageDTO;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现领域服务接口，定义申请、审核、分页查询业务契约。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
public interface WithdrawService {

    /**
     * 商家发起提现申请，落库为 PENDING 状态。
     * @param merchantId 商家ID，来自可信 Header
     * @param dto 提现申请入参
     * @return 提现申请ID
     */
    Long apply(Long merchantId, WithdrawApplyDTO dto);

    /**
     * 商家分页查询自身提现申请，按 ID 倒序游标分页。
     * @param dto 分页入参
     * @return 提现申请分页结果
     */
    CursorPageVO<MerchantWithdrawPO> pageByMerchant(WithdrawPageDTO dto);

    /**
     * 平台分页查询全部提现申请，支持按商家ID与状态过滤。
     * @param dto 分页入参
     * @return 提现申请分页结果
     */
    CursorPageVO<MerchantWithdrawPO> pageAll(WithdrawPageDTO dto);

    /**
     * 平台审核通过，将 PENDING 置为 APPROVED。
     * @param id 提现申请ID
     */
    void approve(Long id);

    /**
     * 平台审核驳回，将 PENDING 置为 REJECTED 并记录原因。
     * @param id 提现申请ID
     * @param reason 驳回原因
     */
    void reject(Long id, String reason);
}
