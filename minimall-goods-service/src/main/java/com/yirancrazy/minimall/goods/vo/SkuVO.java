package com.yirancrazy.minimall.goods.vo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SKU 视图对象，用于用户端商品详情中的规格展示
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkuVO {
    private Long skuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
}
