package com.yirancrazy.minimall.merchant.manager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请 Manager 接口，继承 IService 复用 MyBatis-Plus 通用能力。
 * @Version: 1.1
 * @DateTime: 2026/08/16
 **/
public interface MerchantWithdrawManager extends IService<MerchantWithdrawPO> {

    /**
     * 统计商家在途提现合计（PENDING/APPROVED），用于申请时在途总额校验。
     * @param merchantId 商家ID
     * @return 在途提现合计；无记录返回 0
     */
    BigDecimal sumInFlightAmount(Long merchantId);

    /**
     * 条件更新提现单状态，仅当当前状态为 fromStatus 时原子更新为 toStatus。
     * @param id 提现单ID
     * @param fromStatus 期望的当前状态码（审核仅允许 PENDING）
     * @param toStatus 目标状态码（APPROVED 或 REJECTED）
     * @param reason 驳回原因，审核通过时传 null
     * @param reviewedAt 审核时间
     * @return 影响行数，0 表示状态不匹配（重复审核/终态单）
     */
    int updateStatusIf(Long id, int fromStatus, int toStatus, String reason, LocalDateTime reviewedAt);
}
