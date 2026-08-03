package com.yirancrazy.minimall.pay.mapper;

import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.pay.entity.PayTransactionPO;
import com.yirancrazy.minimall.pay.vo.PayStatementVO;
import com.yirancrazy.minimall.pay.vo.PayStatisticsVO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: MyBatis Mapper 接口，提供数据库映射操作。
 * @Version: 1.0
 * @DateTime: 2026/7/31
 **/
@Mapper
public interface PayTransactionMapper extends BaseMapper<PayTransactionPO> {

    /**
     * 资金统计聚合查询，merchantId 为空时统计全平台，支持时间范围过滤。
     * @param merchantId 商家ID，null 表示全平台
     * @param startTime 起始时间，null 不限制
     * @param endTime 截止时间，null 不限制
     * @return 统计VO
     */
    @Select("<script>"
        + "SELECT COALESCE(SUM(CASE WHEN status = 2 THEN amount END), 0) AS totalAmount, "
        + "COALESCE(SUM(CASE WHEN status = 6 THEN amount END), 0) AS refundAmount, "
        + "COUNT(*) AS transactionCount FROM t_pay_transaction WHERE 1 = 1 "
        + "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if> "
        + "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> "
        + "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if>"
        + "</script>")
    PayStatisticsVO statistics(@Param("merchantId") Long merchantId,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 对账单聚合查询，按状态分组统计交易笔数与金额。
     * @param startTime 起始时间
     * @param endTime 截止时间
     * @return 对账单 VO
     */
    @Select("SELECT "
        + "COUNT(*) AS totalCount, "
        + "COALESCE(SUM(amount), 0) AS totalAmount, "
        + "COALESCE(SUM(CASE WHEN status = 2 THEN amount END), 0) AS paidAmount, "
        + "COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS paidCount, "
        + "COALESCE(SUM(CASE WHEN status = 6 THEN amount END), 0) AS refundedAmount, "
        + "COALESCE(SUM(CASE WHEN status = 6 THEN 1 ELSE 0 END), 0) AS refundedCount, "
        + "COALESCE(SUM(CASE WHEN status = 7 THEN amount END), 0) AS frozenAmount, "
        + "COALESCE(SUM(CASE WHEN status = 7 THEN 1 ELSE 0 END), 0) AS frozenCount "
        + "FROM t_pay_transaction "
        + "WHERE create_time >= #{startTime} AND create_time < #{endTime}")
    PayStatementVO statement(@Param("startTime") LocalDateTime startTime,
                             @Param("endTime") LocalDateTime endTime);
}