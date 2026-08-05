package com.yirancrazy.minimall.merchant.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请出参 VO。
 * @Version: 1.0
 * @DateTime: 2026/08/05
 **/
@Data
public class WithdrawVO {

    private Long id;
    private Long merchantId;
    private String withdrawNo;
    private BigDecimal amount;
    private Integer status;
    private String reason;
    private LocalDateTime appliedAt;
    private LocalDateTime reviewedAt;

    /**
     * 将 PO 转换为 VO。
     * @param po 持久化对象
     * @return 视图对象
     */
    public static WithdrawVO from(MerchantWithdrawPO po) {
        if (po == null) {
            return null;
        }
        WithdrawVO vo = new WithdrawVO();
        vo.setId(po.getId());
        vo.setMerchantId(po.getMerchantId());
        vo.setWithdrawNo(po.getWithdrawNo());
        vo.setAmount(po.getAmount());
        vo.setStatus(po.getStatus());
        vo.setReason(po.getReason());
        vo.setAppliedAt(po.getAppliedAt());
        vo.setReviewedAt(po.getReviewedAt());
        return vo;
    }
}
