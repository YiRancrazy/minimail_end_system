package com.yirancrazy.minimall.api.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSnapshotDTO {
    private Long shopId;
    private String shopName;
    private String status;
}