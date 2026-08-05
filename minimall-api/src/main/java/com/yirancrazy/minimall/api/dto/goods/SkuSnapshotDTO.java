package com.yirancrazy.minimall.api.dto.goods;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SkuSnapshot数据传输对象，用于SkuSnapshot相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SkuSnapshotDTO {
    private Long skuId;
    private Long spuId;
    private String skuName;
    private BigDecimal price;
    private Integer stock;
    private Long merchantId;
}