package com.yirancrazy.minimall.order.manager;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yirancrazy.minimall.order.dto.OrderStatisticsResultDto;
import com.yirancrazy.minimall.order.dto.OrderStatusCountDto;
import com.yirancrazy.minimall.order.dto.OrderSummaryResultDto;
import com.yirancrazy.minimall.order.dto.OrderTrendPointDto;
import com.yirancrazy.minimall.order.entity.OrderPo;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 封装 OrderPo 的持久化访问接口，用于订单主记录。
 *   业务边界：继承 IService 提供基础 CRUD 和条件更新，不包含远程调用或跨聚合业务编排。
 *   使用场景：由订单服务及补偿任务调用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
public interface OrderManager extends IService<OrderPo> {

  /**
   * 执行订单统计 SQL。
   *
   * <p>按商家和时间范围聚合单量、支付金额、退款金额及分状态计数。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param startTime 筛选开始时间，包含边界
   * @param endTime 筛选结束时间，包含边界
   * @return 订单统计数据库投影。
   */
  OrderStatisticsResultDto statistics(
      Long merchantId, LocalDateTime startTime, LocalDateTime endTime);

  /**
   * 按用户统计各订单状态数量。
   *
   * <p>只统计未删除订单并按 status 分组。
   *
   * @param userId 用户 ID，必须对应当前订单归属
   * @return 状态与数量的分组列表。
   */
  List<OrderStatusCountDto> countByStatus(Long userId);

  /**
   * 查询订单经营汇总。
   *
   * <p>起止日期和商家 ID 均可空，格式非法时由服务层降级。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param startTime 筛选开始时间，包含边界
   * @param endTime 筛选结束时间，包含边界
   * @return 订单汇总统一响应。
   */
  OrderSummaryResultDto summary(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);

  /**
   * 查询订单趋势。
   *
   * <p>按日期聚合单量和已支付金额，无数据返回空点位。
   *
   * @param merchantId 商家 ID，用于订单归属或统计范围校验
   * @param startTime 筛选开始时间，包含边界
   * @param endTime 筛选结束时间，包含边界
   * @return 订单趋势统一响应。
   */
  List<OrderTrendPointDto> trend(Long merchantId, LocalDateTime startTime, LocalDateTime endTime);

  /**
   * 按 ID 游标查询支付已过期或缺少过期时间的订单。
   *
   * <p>使用 payExpireAt<=now 或 payExpireAt 为空作为兜底条件，并限制状态和批量大小。
   *
   * @param status 订单状态筛选值或状态编码
   * @param lastId 上一批最后一条订单 ID，用于游标续扫
   * @param pageSize 单批查询数量
   * @return 符合条件的订单列表。
   */
  List<OrderPo> listExpiredPendingOrders(Integer status, Long lastId, Integer pageSize);
}
