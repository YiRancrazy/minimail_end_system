package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载购物车内部计数接口所需的用户标识。请求缺少 userId 时会在控制器委托计数
 *     查询前被校验拒绝。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CartCountDto {

  @NotNull(message = "userId cannot be null")
  private Long userId;
}
