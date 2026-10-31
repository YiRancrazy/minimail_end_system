package com.yirancrazy.minimall.order.dto;

import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单状态分组计数的单行数据库结果。
 *   业务边界：status 为订单状态整数编码，cnt 为该状态订单数量；不包含用户或时间范围。
 *   使用场景：OrderMapper.countByStatus 返回后由 OrderServiceImpl 聚合为用户状态计数。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class OrderStatusCountDto {

  private Integer status;

  private Long cnt;
}
