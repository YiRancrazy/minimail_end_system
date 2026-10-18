package com.yirancrazy.minimall.pay.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.dto.PayStatementResultDTO;
import com.yirancrazy.minimall.pay.dto.PayStatisticsDTO;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;



/**
 * @Author: yirancrazy@gmail.com
 * @Description: PayTransactionMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransactionPO> {


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Select("<script>"
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
    PayStatisticsDTO statistics(@Param("merchantId") Long merchantId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);


    /**
     * 更新业务数据。
     * @param paymentNo paymentNo 参数
     * @param fromStatus fromStatus 参数
     * @param toStatus toStatus 参数
     * @return 业务数值
     */
    @Update("UPDATE t_pay_transaction SET status = #{toStatus}, update_time = NOW() "
        + "WHERE payment_no = #{paymentNo} AND status = #{fromStatus}")
    int updateStatusIf(@Param("paymentNo") String paymentNo,
                       @Param("fromStatus") int fromStatus,
                       @Param("toStatus") int toStatus);


    /**
     * 执行 sumSettledAmount 方法。
     * @param merchantId merchantId 参数
     * @return 处理结果
     */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM t_pay_transaction "
        + "WHERE merchant_id = #{merchantId} AND status IN (2, 5, 6) AND is_deleted = 0")
    BigDecimal sumSettledAmount(@Param("merchantId") Long merchantId);


    /**
     * 执行 statement 方法。
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Select("SELECT "
        + "COUNT(*) AS totalCount, "
        + "COALESCE(SUM(t.amount), 0) AS totalAmount, "
        + "COALESCE(SUM(CASE WHEN t.status = 2 THEN t.amount END), 0) AS paidAmount, "
        + "COALESCE(SUM(CASE WHEN t.status = 2 THEN 1 ELSE 0 END), 0) AS paidCount, "
        + "COALESCE(SUM(COALESCE(r.refunded_amount, 0)), 0) AS refundedAmount, "
        + "COALESCE(SUM(CASE WHEN r.refunded_amount IS NOT NULL THEN 1 ELSE 0 END), 0) AS refundedCount, "
        + "COALESCE(SUM(CASE WHEN t.status = 7 THEN t.amount END), 0) AS frozenAmount, "
        + "COALESCE(SUM(CASE WHEN t.status = 7 THEN 1 ELSE 0 END), 0) AS frozenCount "
        + "FROM t_pay_transaction t LEFT JOIN ("
        + "SELECT payment_no, SUM(amount) AS refunded_amount FROM t_pay_refund "
        + "WHERE status = 1 AND is_deleted = 0 GROUP BY payment_no) r "
        + "ON r.payment_no = t.payment_no "
        + "WHERE t.create_time >= #{startTime} AND t.create_time < #{endTime}")
    PayStatementResultDTO statement(@Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
}
