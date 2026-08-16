package com.yirancrazy.minimall.merchant.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.merchant.entity.MerchantWithdrawPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商家提现申请 Mapper，继承 MyBatis-Plus BaseMapper。
 * @Version: 1.1
 * @DateTime: 2026/08/16
 **/
public interface MerchantWithdrawMapper extends BaseMapper<MerchantWithdrawPO> {

    /**
     * 统计商家在途提现合计：待审核（PENDING）与已通过（APPROVED）提现单金额求和，
     * 用于申请时在途总额校验；已拒绝/已打款的终态单不计入。
     * @param merchantId 商家ID
     * @return 在途提现合计；无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_merchant_withdraw "
        + "WHERE merchant_id = #{merchantId} AND status IN (1, 2) AND is_deleted = 0")
    BigDecimal sumInFlightAmount(@Param("merchantId") Long merchantId);

    /**
     * 条件更新提现单状态：仅当当前状态为 fromStatus 时原子更新为 toStatus，返回影响行数。
     * 行级条件保证审核并发/重复点击只放行一次，杜绝已审核终态单被翻转；驳回原因仅在非空时写入。
     * @param id 提现单ID
     * @param fromStatus 期望的当前状态码（审核仅允许 PENDING）
     * @param toStatus 目标状态码（APPROVED 或 REJECTED）
     * @param reason 驳回原因，审核通过时传 null
     * @param reviewedAt 审核时间
     * @return 影响行数，0 表示状态不匹配（重复审核/终态单）
     */
    @Update("UPDATE t_merchant_withdraw SET status = #{toStatus}, "
        + "reason = COALESCE(#{reason}, reason), reviewed_at = #{reviewedAt}, update_time = NOW() "
        + "WHERE id = #{id} AND status = #{fromStatus} AND is_deleted = 0")
    int updateStatusIf(@Param("id") Long id,
                       @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus,
                       @Param("reason") String reason,
                       @Param("reviewedAt") LocalDateTime reviewedAt);
}
