package com.yirancrazy.minimall.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDto;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDto;
import com.yirancrazy.minimall.pay.entity.PayTransactionPo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 映射支付交易表的统计、条件更新和对账 SQL。
 * 所有状态推进使用带原状态条件的更新，避免并发回调或退款覆盖新状态。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransactionPo> {

  /**
   * 按创建时间聚合支付成功总额、退款总额和交易笔数。
   * 退款金额只统计成功退款记录，并支持可选商家与时间条件。
   *
   * @param merchantId 商家筛选条件，可为空
   * @param startTime 创建时间下界，可为空
   * @param endTime 创建时间上界，可为空
   * @return 支付统计数据
   */
  @Select(
      "<script>"
          + "SELECT COALESCE(SUM(CASE WHEN t.status = 2 THEN t.amount END), 0) AS totalAmount, "
          + "COALESCE(SUM(CASE WHEN t.status = 2 THEN COALESCE(r.refunded_amount, 0) END), 0) "
          + "AS refundAmount, COUNT(*) AS transactionCount "
          + "FROM t_pay_transaction t LEFT JOIN ("
          + "SELECT payment_no, SUM(amount) AS refunded_amount FROM t_pay_refund "
          + "WHERE status = 1 AND is_deleted = 0 GROUP BY payment_no) r "
          + "ON r.payment_no = t.payment_no WHERE 1 = 1 "
          + "<if test='merchantId != null'>AND t.merchant_id = #{merchantId}</if> "
          + "<if test='startTime != null'>AND t.create_time &gt;= #{startTime}</if> "
          + "<if test='endTime != null'>AND t.create_time &lt;= #{endTime}</if>"
          + "</script>")
  PayStatisticsDto statistics(
      @Param("merchantId") Long merchantId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /**
   * 使用原状态条件原子推进支付单，并同步数据库更新时间。
   *
   * @param paymentNo 支付单号
   * @param fromStatus 期望的原状态 code
   * @param toStatus 目标状态 code
   * @return 实际更新行数，0 表示并发状态冲突
   */
  @Update(
      "UPDATE t_pay_transaction SET status = #{toStatus}, update_time = NOW() "
          + "WHERE payment_no = #{paymentNo} AND status = #{fromStatus}")
  int updateStatusIf(
      @Param("paymentNo") String paymentNo,
      @Param("fromStatus") int fromStatus,
      @Param("toStatus") int toStatus);

  /**
   * 汇总商家已成功、退款中及已退款流水的原始交易金额。
   *
   * @param merchantId 商家 ID
   * @return 已结算口径金额；无记录时返回 0
   */
  @Select(
      "SELECT COALESCE(SUM(amount), 0) FROM t_pay_transaction "
          + "WHERE merchant_id = #{merchantId} AND status IN (2, 5, 6) AND is_deleted = 0")
  BigDecimal sumSettledAmount(@Param("merchantId") Long merchantId);

  /**
   * 按左闭右开时间范围聚合每日对账单所需的支付、退款和冻结指标。
   *
   * @param startTime 统计下界
   * @param endTime 统计上界
   * @return 对账单聚合结果
   */
  @Select(
      "SELECT COUNT(*) AS totalCount, COALESCE(SUM(t.amount), 0) AS totalAmount, COALESCE(SUM(CASE"
          + " WHEN t.status = 2 THEN t.amount END), 0) AS paidAmount, COALESCE(SUM(CASE WHEN"
          + " t.status = 2 THEN 1 ELSE 0 END), 0) AS paidCount,"
          + " COALESCE(SUM(COALESCE(r.refunded_amount, 0)), 0) AS refundedAmount, COALESCE(SUM(CASE"
          + " WHEN r.refunded_amount IS NOT NULL THEN 1 ELSE 0 END), 0) AS refundedCount,"
          + " COALESCE(SUM(CASE WHEN t.status = 7 THEN t.amount END), 0) AS frozenAmount,"
          + " COALESCE(SUM(CASE WHEN t.status = 7 THEN 1 ELSE 0 END), 0) AS frozenCount FROM"
          + " t_pay_transaction t LEFT JOIN (SELECT payment_no, SUM(amount) AS refunded_amount FROM"
          + " t_pay_refund WHERE status = 1 AND is_deleted = 0 GROUP BY payment_no) r ON"
          + " r.payment_no = t.payment_no WHERE t.create_time >= #{startTime} AND t.create_time <"
          + " #{endTime}")
  PayStatementResultDto statement(
      @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
