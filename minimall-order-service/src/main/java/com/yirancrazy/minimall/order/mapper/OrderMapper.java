package com.yirancrazy.minimall.order.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yirancrazy.minimall.order.entity.OrderPO;
import com.yirancrazy.minimall.order.vo.OrderStatisticsVO;
import com.yirancrazy.minimall.order.vo.OrderSummaryVO;
import com.yirancrazy.minimall.order.vo.OrderTrendPointVO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: Order MyBatis Mapper 接口，映射Order表
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public interface OrderMapper extends BaseMapper<OrderPO> {

    /**
     * 聚合统计订单：总数、总金额（净额口径：剔除退款中/已退款）、退款金额（退款中+已退款）、各状态计数。
     * @param merchantId 商家ID，null 表示全平台
     * @param startTime 起始时间
     * @param endTime 截止时间
     * @return 订单统计VO
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
    OrderStatisticsVO statistics(@Param("merchantId") Long merchantId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 按状态统计指定用户订单数，返回 status→cnt 行。
     * @param userId 用户ID
     * @return status 与订单数量映射行列表
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM t_order "
        + "WHERE user_id = #{userId} AND is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("userId") Long userId);

    /**
     * 平台财务汇总：区间订单总数、GMV（已支付口径）、待支付金额、退款金额与净收入。
     * @param merchantId 商家ID，null 表示全平台
     * @param startTime 起始时间
     * @param endTime 截止时间
     * @return 订单财务汇总VO
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
    OrderSummaryVO summary(@Param("merchantId") Long merchantId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);

    /**
     * 平台订单趋势：按日聚合区间订单数与已支付金额，按日期升序。
     * @param merchantId 商家ID，null 表示全平台
     * @param startTime 起始时间
     * @param endTime 截止时间
     * @return 按日聚合数据点列表
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
    List<OrderTrendPointVO> trend(@Param("merchantId") Long merchantId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
}
