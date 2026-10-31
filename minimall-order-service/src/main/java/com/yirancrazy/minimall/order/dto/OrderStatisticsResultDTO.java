package com.yirancrazy.minimall.order.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单 Mapper 聚合后的总单量、金额和分状态数量。
 *   业务边界：属于数据库查询投影，不包含展示转换；receivedCount 对应已完成订单，cancelledCount 对应已取消订单。
 *   使用场景：OrderManagerImpl 转换为 OrderStatisticsVo 后供平台统计接口使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResultDto {

  private Long totalOrderCount;
  private BigDecimal totalAmount;
  private BigDecimal refundAmount;
  private Long pendingCount;
  private Long paidCount;
  private Long shippedCount;
  private Long receivedCount;
  private Long cancelledCount;
  private Long refundingCount;
  private Long refundedCount;
}
