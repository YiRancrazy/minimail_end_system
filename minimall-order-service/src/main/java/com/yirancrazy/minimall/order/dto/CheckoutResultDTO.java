package com.yirancrazy.minimall.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载订单结算编排后的订单 ID、订单组号和购物车清理任务 ID。
 *   业务边界：作为订单服务内部返回值使用，不直接持久化；cleanupTaskId 为空表示本次无需清理购物车。
 *   使用场景：OrderService.checkout 返回后由 OrderCheckoutServiceImpl 触发立即清理。
 * @Version: 1.0
 * @DateTime: 2026/10/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutResultDto {

    private Long orderId;

    private String orderGroupNo;

    private Long cartCleanupTaskId;
}
