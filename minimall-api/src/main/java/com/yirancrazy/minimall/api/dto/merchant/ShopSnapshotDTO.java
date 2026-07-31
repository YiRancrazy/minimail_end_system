package com.yirancrazy.minimall.api.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopSnapshot数据传输对象，用于ShopSnapshot相关数据传输
 * @Version: 1.0
 * @DateTime: 2026/07/31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopSnapshotDTO {
    private Long shopId;
    private String shopName;
    private String status;
}