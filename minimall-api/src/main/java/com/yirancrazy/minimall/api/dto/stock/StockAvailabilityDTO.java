package com.yirancrazy.minimall.api.dto.stock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SKU可用库存快照。
 * @Version: 1.0
 * @DateTime: 2026/10/20
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockAvailabilityDTO {

    private Long skuId;
    private Long available;
}
