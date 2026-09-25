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
 * @Description: MyBatis Mapper 接口，提供数据库映射操作。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Mapper
public interface PayRefundMapper extends BaseMapper<PayRefundPO> {

    /**
     * 按退款请求幂等键查询退款记录。
     * @param refundRequestNo 退款请求幂等键
     * @return 退款记录；不存在返回 null
     */
    @Select("SELECT * FROM t_pay_refund WHERE refund_request_no = #{refundRequestNo} "
        + "AND is_deleted = 0 LIMIT 1")
    PayRefundPO findByRefundRequestNo(@Param("refundRequestNo") String refundRequestNo);

    /**
     * 查询到达重试时间的 PENDING 退款记录。
     * @param now 当前时间
     * @param limit 单批数量
     * @return 待补偿退款列表
     */
    @Select("SELECT * FROM t_pay_refund WHERE status = 0 AND is_deleted = 0 "
        + "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) "
        + "ORDER BY id ASC LIMIT #{limit}")
    List<PayRefundPO> selectPendingRefunds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * 抢占待补偿退款，避免多实例同时处理同一退款。
     * @param refundNo 退款单号
     * @param now 当前时间
     * @param leaseUntil 本次处理租约到期时间
     * @return 影响行数；0 表示已被其他节点抢占或状态已变更
     */
    @Update("UPDATE t_pay_refund SET next_retry_at = #{leaseUntil}, "
        + "retry_count = COALESCE(retry_count, 0) + 1 "
        + "WHERE refund_no = #{refundNo} AND status = 0 AND is_deleted = 0 "
        + "AND (next_retry_at IS NULL OR next_retry_at <= #{now})")
    int claimPendingRefund(@Param("refundNo") String refundNo, @Param("now") LocalDateTime now,
                           @Param("leaseUntil") LocalDateTime leaseUntil);

    /**
     * 条件更新退款状态，保证成功和通知副作用只执行一次。
     * @param refundNo 退款单号
     * @param fromStatus 原状态
     * @param toStatus 目标状态
     * @param refundTradeNo 渠道退款交易号
     * @return 影响行数
     */
    @Update("UPDATE t_pay_refund SET status = #{toStatus}, refund_trade_no = #{refundTradeNo}, "
        + "next_retry_at = NULL WHERE refund_no = #{refundNo} AND status = #{fromStatus} "
        + "AND is_deleted = 0")
    int updateStatusIf(@Param("refundNo") String refundNo, @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus, @Param("refundTradeNo") String refundTradeNo);

    /**
     * 安排下一次退款补偿时间。
     * @param refundNo 退款单号
     * @param nextRetryAt 下一次补偿时间
     * @return 影响行数
     */
    @Update("UPDATE t_pay_refund SET next_retry_at = #{nextRetryAt}, "
        + "retry_count = COALESCE(retry_count, 0) + 1 "
        + "WHERE refund_no = #{refundNo} AND status = 0 AND is_deleted = 0")
    int scheduleRefundRetry(@Param("refundNo") String refundNo,
                            @Param("nextRetryAt") LocalDateTime nextRetryAt);

    /**
     * 统计指定支付单已占用退款金额（在途待发起 PENDING + 已成功 SUCCESS），用于累计退款上限校验，
     * 失败/关闭的退款记录不计入，避免并发部分退款累计超退。
     * @param paymentNo 支付单号
     * @return 已占用退款金额；无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_pay_refund "
        + "WHERE payment_no = #{paymentNo} AND status IN (0, 1) AND is_deleted = 0")
    BigDecimal sumCommittedAmount(@Param("paymentNo") String paymentNo);

    /**
     * 统计商家退款扣减合计：该商家支付流水关联的退款中（PENDING）与退款成功（SUCCESS）金额求和，
     * 用于可提现余额计算；退款失败/关闭不计入，防止退款未实际退回时错误扣减可提现余额。
     * @param merchantId 商家ID
     * @return 退款扣减合计；无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(r.amount), 0) FROM t_pay_refund r "
        + "INNER JOIN t_pay_transaction t ON t.payment_no = r.payment_no "
        + "WHERE t.merchant_id = #{merchantId} AND r.status IN (0, 1) "
        + "AND r.is_deleted = 0 AND t.is_deleted = 0")
    BigDecimal sumDeductAmount(@Param("merchantId") Long merchantId);
}
