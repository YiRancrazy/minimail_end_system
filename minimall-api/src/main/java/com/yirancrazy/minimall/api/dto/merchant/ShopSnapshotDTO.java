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
    /** 归属商家ID，供下游服务校验店铺归属，防越权绑定他人店铺 */
    private Long merchantId;
    private String shopName;
    private String status;
}