package com.yirancrazy.minimall.user.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏出参VO，用于收藏列表展示
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteVO {
    private Long id;
    private Long userId;
    private Long skuId;
    private LocalDateTime createTime;
}
