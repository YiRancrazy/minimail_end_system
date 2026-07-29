package com.yirancrazy.minimall.api.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 店铺跨服务快照 DTO，承载店铺标识、店主、店铺名与状态。
 * @Version: 1.0
 * @DateTime: 2026/7/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSnapshotDTO {
    private Long shopId;
    private String shopName;
    private String status;
}