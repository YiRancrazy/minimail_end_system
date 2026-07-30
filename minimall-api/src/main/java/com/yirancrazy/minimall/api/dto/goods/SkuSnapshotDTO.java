package com.yirancrazy.minimall.api.dto.goods;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
* 商品 SKU 跨服务快照 DTO，承载 SKU 标识、SPU 标识、名称、价格与库存。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkuSnapshotDTO {
    private Long skuId;
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
}