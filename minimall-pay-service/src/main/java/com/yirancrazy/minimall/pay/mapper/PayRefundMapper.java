package com.yirancrazy.minimall.pay.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.entity.PayRefundPO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayRefundMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Mapper
public interface PayRefundMapper extends BaseMapper<PayRefundPO> {


    /**
     * 执行 findByRefundRequestNo 方法。
     * @param refundRequestNo refundRequestNo 参数
     * @return 处理结果
     */
    @Select("SELECT * FROM t_pay_refund WHERE refund_request_no = #{refundRequestNo} "
        + "AND is_deleted = 0 LIMIT 1")
    PayRefundPO findByRefundRequestNo(@Param("refundRequestNo") String refundRequestNo);


    /**
     * 执行 selectPendingRefunds 方法。
     * @param now now 参数
     * @param limit limit 参数
     * @return 结果列表
     */
    @Select("SELECT * FROM t_pay_refund WHERE status = 0 AND is_deleted = 0 "
        + "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) "
        + "ORDER BY id ASC LIMIT #{limit}")
    List<PayRefundPO> selectPendingRefunds(@Param("now") LocalDateTime now, @Param("limit") int limit);


    /**
     * 执行 claimPendingRefund 方法。
     * @param refundNo refundNo 参数
     * @param now now 参数
     * @param leaseUntil leaseUntil 参数
     * @return 业务数值
     */
    @Update("UPDATE t_pay_refund SET next_retry_at = #{leaseUntil}, "
        + "retry_count = COALESCE(retry_count, 0) + 1 "
        + "WHERE refund_no = #{refundNo} AND status = 0 AND is_deleted = 0 "
        + "AND (next_retry_at IS NULL OR next_retry_at <= #{now})")
    int claimPendingRefund(@Param("refundNo") String refundNo, @Param("now") LocalDateTime now,
                           @Param("leaseUntil") LocalDateTime leaseUntil);


    /**
     * 更新业务数据。
     * @param refundNo refundNo 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @param refundTradeNo refundTradeNo 参数
     * @return 业务数值
     */
    @Update("UPDATE t_pay_refund SET status = #{toStatus}, refund_trade_no = #{refundTradeNo}, "
        + "next_retry_at = NULL WHERE refund_no = #{refundNo} AND status = #{fromStatus} "
        + "AND is_deleted = 0")
    int updateStatusIf(@Param("refundNo") String refundNo, @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus, @Param("refundTradeNo") String refundTradeNo);


    /**
     * 执行 scheduleRefundRetry 方法。
     * @param refundNo refundNo 参数
     * @param nextRetryAt nextRetryAt 参数
     * @return 业务数值
     */
    @Update("UPDATE t_pay_refund SET next_retry_at = #{nextRetryAt}, "
        + "retry_count = COALESCE(retry_count, 0) + 1 "
        + "WHERE refund_no = #{refundNo} AND status = 0 AND is_deleted = 0")
    int scheduleRefundRetry(@Param("refundNo") String refundNo,
                            @Param("nextRetryAt") LocalDateTime nextRetryAt);


    /**
     * 执行 sumCommittedAmount 方法。
     * @param paymentNo paymentNo 参数
     * @return 处理结果
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_pay_refund "
        + "WHERE payment_no = #{paymentNo} AND status IN (0, 1) AND is_deleted = 0")
    BigDecimal sumCommittedAmount(@Param("paymentNo") String paymentNo);


    /**
     * 执行 sumDeductAmount 方法。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Select("SELECT COALESCE(SUM(r.amount), 0) FROM t_pay_refund r "
        + "INNER JOIN t_pay_transaction t ON t.payment_no = r.payment_no "
        + "WHERE t.merchant_id = #{merchantId} AND r.status IN (0, 1) "
        + "AND r.is_deleted = 0 AND t.is_deleted = 0")
    BigDecimal sumDeductAmount(@Param("merchantId") Long merchantId);
}
