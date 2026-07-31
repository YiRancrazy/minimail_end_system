package com.yirancrazy.minimall.api.dto.goods;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuSnapshotDTO description.
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
public class SkuSnapshotDTO {
    private Long skuId;
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
}