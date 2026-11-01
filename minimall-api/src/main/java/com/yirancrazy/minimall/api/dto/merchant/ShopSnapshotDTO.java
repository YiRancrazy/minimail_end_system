package com.yirancrazy.minimall.api.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品发布校验使用的店铺快照，包含店铺、商家和营业状态.
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopSnapshotDto {
    private Long shopId;

    private Long merchantId;
    private String shopName;
    private String status;
}
