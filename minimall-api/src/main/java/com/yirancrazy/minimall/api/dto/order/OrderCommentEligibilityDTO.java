package com.yirancrazy.minimall.api.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 订单服务返回的评价资格判定结果及订单归属信息.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommentEligibilityDto {

  private boolean eligible;

  private Long orderId;

  private Long userId;

  private Long merchantId;

  private Integer status;
}
