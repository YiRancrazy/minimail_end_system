package com.yirancrazy.minimall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.yirancrazy.minimall.common.base.BasePO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: UserFavorite持久化对象，映射用户商品收藏表t_user_favorite
 * @Version: 1.0
 * @DateTime: 2026/08/03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_favorite")
public class UserFavoritePO extends BasePO {
    private Long userId;
    private Long skuId;
}
