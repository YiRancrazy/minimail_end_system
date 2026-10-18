package com.yirancrazy.minimall.api.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: ShopSnapshotDTO 数据传输对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShopSnapshotDTO {
    private Long shopId;

    private Long merchantId;
    private String shopName;
    private String status;
}