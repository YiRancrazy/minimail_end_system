package com.yirancrazy.minimall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载已弃用的单 SKU 直接购买请求。
 *   业务边界：保留 skuId、quantity 和可选收货信息以兼容旧入口，新结算应使用 OrderCheckoutDto。
 *   使用场景：UserOrderControllerV1 的 Deprecated create 接口使用。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDto {

  @NotNull(message = "商品SKU ID不能为空")
  private Long skuId;

  @NotNull(message = "购买数量不能为空")
  @Min(value = 1, message = "购买数量至少为1")
  private Integer quantity;

  @Valid private ReceiverDto receiver;
}
