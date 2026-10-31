package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yirancrazy.minimall.common.base.BasePo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户商品收藏持久化模型，映射 t_user_favorite，以用户 ID 和 SKU ID 标识一条收藏关系.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_favorite")
public class UserFavoritePo extends BasePo {
  private Long userId;
  private Long skuId;
}
