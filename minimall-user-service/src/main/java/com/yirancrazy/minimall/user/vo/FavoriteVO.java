package com.yirancrazy.minimall.user.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 收藏列表接口视图，组合收藏关系、SKU 快照和 SPU 快照后的商品展示字段.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@NoArgsConstructor
public class FavoriteVo {

    private Long id;

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String skuImage;

    private BigDecimal price;

    private LocalDateTime createTime;
}
