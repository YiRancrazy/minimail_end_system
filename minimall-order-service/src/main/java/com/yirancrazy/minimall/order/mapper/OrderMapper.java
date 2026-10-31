package com.yirancrazy.minimall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDto;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDto;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDto;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDto;
import com.yirancrazy.minimall.order.entity.OrderPo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 提供 OrderPo 的 MyBatis-Plus 基础数据访问能力。
 *   业务边界：只声明数据库查询和条件更新接口，不承载业务规则或跨服务调用。
 *   使用场景：由对应 Manager 注入并调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OrderMapper extends BaseMapper<OrderPo> {

  /**
   * 执行订单统计 SQL。
   *
   * <p>按商家和时间范围聚合单量、金额及分状态计数。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param startTime 筛选开始时间，包含边界
   * @param endTime 筛选结束时间，包含边界
   * @return 订单统计数据库投影。
   */
  @Select(
      "<script>"
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
  OrderStatisticsResultDto statistics(
      @Param("merchantId") Long merchantId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /**
   * 按用户统计各订单状态数量。
   *
   * <p>只处理未删除订单并按状态分组。
   *
   * @param userId 用户 ID，必须对应当前订单归属
   * @return 状态与数量分组列表。
   */
  @Select(
      "SELECT status, COUNT(*) AS cnt FROM t_order "
          + "WHERE user_id = #{userId} AND is_deleted = 0 GROUP BY status")
  List<OrderStatusCountDto> countByStatus(@Param("userId") Long userId);

  /**
   * 执行订单经营汇总 SQL。
   *
   * <p>聚合总单量、支付金额、待支付金额、退款金额和净收入。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param startTime 筛选开始时间，包含边界
   * @param endTime 筛选结束时间，包含边界
   * @return 订单汇总数据库投影。
   */
  @Select(
      "<script>"
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
  OrderSummaryResultDto summary(
      @Param("merchantId") Long merchantId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /**
   * 按创建日期聚合订单趋势。
   *
   * <p>返回每日单量和已支付口径金额，并按日期升序。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param startTime 筛选开始时间，包含边界
   * @param endTime 筛选结束时间，包含边界
   * @return 订单趋势数据库投影列表。
   */
  @Select(
      "<script>"
          + "SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS count, "
          + "COALESCE(SUM(CASE WHEN status IN (2,3,4) THEN amount END), 0) AS amount "
          + "FROM t_order WHERE is_deleted = 0 "
          + "<if test='merchantId != null'>AND merchant_id = #{merchantId}</if> "
          + "<if test='startTime != null'>AND create_time &gt;= #{startTime}</if> "
          + "<if test='endTime != null'>AND create_time &lt;= #{endTime}</if> "
          + "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY date"
          + "</script>")
  List<OrderTrendPointDto> trend(
      @Param("merchantId") Long merchantId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);
}
