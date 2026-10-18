package com.yirancrazy.minimall.user.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: FavoriteVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@NoArgsConstructor
public class FavoriteVO {

    private Long id;

    private Long skuId;

    private Long spuId;

    private String skuName;

    private String skuImage;

    private BigDecimal price;

    private LocalDateTime createTime;
}
