package com.yirancrazy.minimall.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.entity.PayRefundPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射退款流水表的查询与原子更新 SQL。
 * 支撑退款幂等、待处理抢占、重试排期及已提交或待处理金额计算。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface PayRefundMapper extends BaseMapper<PayRefundPo> {

    /**
     * 查询未删除的退款请求号对应记录，保证调用方重试时复用同一退款。
     *
     * @param refundRequestNo 调用方退款请求号
     * @return 退款流水；不存在时返回 null
     */
    @Select(
            "SELECT * FROM t_pay_refund WHERE refund_request_no = #{refundRequestNo} "
                    + "AND is_deleted = 0 LIMIT 1")
    PayRefundPo findByRefundRequestNo(@Param("refundRequestNo") String refundRequestNo);

    /**
     * 查询状态为待发起且已到重试时间的退款流水。
     *
     * @param now 当前时间，作为重试时间上界
     * @param limit 单批最大条数
     * @return 按主键升序排列的待处理退款
     */
    @Select(
            "SELECT * FROM t_pay_refund WHERE status = 0 AND is_deleted = 0 "
                    + "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) "
                    + "ORDER BY id ASC LIMIT #{limit}")
    List<PayRefundPo> selectPendingRefunds(
            @Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 通过重试时间条件抢占退款流水，延长租约并累加重试次数。
     *
     * @param refundNo 退款单号
     * @param now 当前时间
     * @param leaseUntil 本次处理租约截止时间
     * @return 1 表示抢占成功，0 表示已被其他任务占用
     */
    @Update(
            "UPDATE t_pay_refund SET next_retry_at = #{leaseUntil}, "
                    + "retry_count = COALESCE(retry_count, 0) + 1 "
                    + "WHERE refund_no = #{refundNo} AND status = 0 AND is_deleted = 0 "
                    + "AND (next_retry_at IS NULL OR next_retry_at <= #{now})")
    int claimPendingRefund(
            @Param("refundNo") String refundNo,
            @Param("now") LocalDateTime now,
            @Param("leaseUntil") LocalDateTime leaseUntil);

    /**
     * 按原退款状态原子更新状态、渠道退款号并清除重试时间。
     *
     * @param refundNo 退款单号
     * @param fromStatus 期望的原状态 code
     * @param toStatus 目标状态 code
     * @param refundTradeNo 渠道退款交易号
     * @return 实际更新行数
     */
    @Update(
            "UPDATE t_pay_refund SET status = #{toStatus}, refund_trade_no = #{refundTradeNo}, "
                    + "next_retry_at = NULL WHERE refund_no = #{refundNo} AND status = #{fromStatus} "
                    + "AND is_deleted = 0")
    int updateStatusIf(
            @Param("refundNo") String refundNo,
            @Param("fromStatus") int fromStatus,
            @Param("toStatus") int toStatus,
            @Param("refundTradeNo") String refundTradeNo);

    /**
     * 为仍处于待发起的退款写入下次重试时间并累加重试次数。
     *
     * @param refundNo 退款单号
     * @param nextRetryAt 下次可处理时间
     * @return 实际更新行数
     */
    @Update(
            "UPDATE t_pay_refund SET next_retry_at = #{nextRetryAt}, "
                    + "retry_count = COALESCE(retry_count, 0) + 1 "
                    + "WHERE refund_no = #{refundNo} AND status = 0 AND is_deleted = 0")
    int scheduleRefundRetry(
            @Param("refundNo") String refundNo, @Param("nextRetryAt") LocalDateTime nextRetryAt);

    /**
     * 汇总支付单下待发起及成功退款的金额。
     *
     * @param paymentNo 支付单号
     * @return 已占用退款金额；无记录时返回 0
     */
    @Select(
            "SELECT COALESCE(SUM(amount), 0) FROM t_pay_refund "
                    + "WHERE payment_no = #{paymentNo} AND status IN (0, 1) AND is_deleted = 0")
    BigDecimal sumCommittedAmount(@Param("paymentNo") String paymentNo);

    /**
     * 汇总商家支付流水关联的待处理及成功退款金额。
     *
     * @param merchantId 商家 ID
     * @return 商家退款扣减金额；无记录时返回 0
     */
    @Select(
            "SELECT COALESCE(SUM(r.amount), 0) FROM t_pay_refund r "
                    + "INNER JOIN t_pay_transaction t ON t.payment_no = r.payment_no "
                    + "WHERE t.merchant_id = #{merchantId} AND r.status IN (0, 1) "
                    + "AND r.is_deleted = 0 AND t.is_deleted = 0")
    BigDecimal sumDeductAmount(@Param("merchantId") Long merchantId);
}
