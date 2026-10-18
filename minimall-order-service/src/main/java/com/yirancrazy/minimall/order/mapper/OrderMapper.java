package com.yirancrazy.minimall.order.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDTO;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDTO;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDTO;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDTO;
import com.yirancrazy.minimall.order.entity.OrderPO;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: OrderMapper 数据访问接口，映射数据库操作。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
public interface OrderMapper extends BaseMapper<OrderPO> {


    /**
     * 执行 statistics 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Select("<script>"
        + "SELECT COUNT(*) AS totalOrderCount, "
        + "COALESCE(SUM(CASE WHEN status IN (2,3,4) THEN amount END), 0) AS totalAmount, "
        + "COALESCE(SUM(CASE WHEN status IN (6,7) THEN amount END), 0) AS refundAmount, "
        + "COALESCE(SUM(CASE WHEN status = 1 THEN 1 END), 0) AS pendingCount, "
        + "COALESCE(SUM(CASE WHEN status = 2 THEN 1 END), 0) AS paidCount, "
        + "COALESCE(SUM(CASE WHEN status = 3 THEN 1 END), 0) AS shippedCount, "
        + "COALESCE(SUM(CASE WHEN status = 4 THEN 1 END), 0) AS receivedCount, "
        + "COALESCE(SUM(CASE WHEN status = 5 THEN 1 END), 0) AS cancelledCount, "
        + "COALESCE(SUM(CASE WHEN status = 6 THEN 1 END), 0) AS refundingCount, "
        + "COALESCE(SUM(CASE WHEN status = 7 THEN 1 END), 0) AS refundedCount "
        + "FROM t_order WHERE 1 = 1 "
        + "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if> "
        + "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> "
        + "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if>"
        + "</script>")
    OrderStatisticsResultDTO statistics(@Param("merchantId") Long merchantId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);


    /**
     * 统计业务数据。
     * @param userId userId 参数
     * @return 结果列表
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM t_order "
        + "WHERE user_id = #{userId} AND is_deleted = 0 GROUP BY status")
    List<OrderStatusCountDTO> countByStatus(@Param("userId") Long userId);


    /**
     * 执行 summary 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 处理结果
     */
    @Select("<script>"
        + "SELECT COUNT(*) AS totalCount, "
        + "COALESCE(SUM(CASE WHEN status IN (2,3,4) THEN amount END), 0) AS totalAmount, "
        + "COALESCE(SUM(CASE WHEN status IN (2,3,4) THEN amount END), 0) AS paidAmount, "
        + "COALESCE(SUM(CASE WHEN status = 1 THEN amount END), 0) AS pendingAmount, "
        + "COALESCE(SUM(CASE WHEN status IN (6,7) THEN amount END), 0) AS refundAmount, "
        + "COALESCE(SUM(CASE WHEN status IN (2,3,4) THEN amount END), 0) "
        + "  - COALESCE(SUM(CASE WHEN status IN (6,7) THEN amount END), 0) AS netIncome "
        + "FROM t_order WHERE is_deleted = 0 "
        + "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if> "
        + "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> "
        + "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if>"
        + "</script>")
    OrderSummaryResultDTO summary(@Param("merchantId") Long merchantId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);


    /**
     * 执行 trend 方法。
     * @param merchantId merchantId 参数
     * @param startTime startTime 参数
     * @param endTime endTime 参数
     * @return 结果列表
     */
    @Select("<script>"
        + "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS count, "
        + "COALESCE(SUM(CASE WHEN status IN (2,3,4) THEN amount END), 0) AS amount "
        + "FROM t_order WHERE is_deleted = 0 "
        + "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if> "
        + "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> "
        + "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if> "
        + "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY date"
        + "</script>")
    List<OrderTrendPointDTO> trend(@Param("merchantId") Long merchantId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
}
