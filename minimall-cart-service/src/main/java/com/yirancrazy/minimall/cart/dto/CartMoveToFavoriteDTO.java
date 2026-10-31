package com.yirancrazy.minimall.cart.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 承载待移入收藏夹的购物车条目主键。校验要求列表非空；条目归属和存在性由
 *     CartService 负责检查。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CartMoveToFavoriteDto {

  @NotEmpty(message = "购物车条目不能为空")
  private List<Long> itemIds;
}
