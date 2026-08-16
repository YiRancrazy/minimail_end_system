package com.yirancrazy.minimall.cart.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 移入收藏夹入参：购物车条目 ID 列表，按条目精确删除，避免按 userId+skuId 误删并发加购的同 SKU 新条目。
 * @Version: 1.0
 * @DateTime: 2026/08/16
 **/
@Data
public class CartMoveToFavoriteDTO {

    /** 购物车条目 ID 列表，必须非空 */
    @NotEmpty(message = "购物车条目不能为空")
    private List<Long> itemIds;
}
