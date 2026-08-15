package com.yirancrazy.minimall.cart.vo;

import java.math.BigDecimal;
import lombok.Data;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 购物车项VO，供Controller边界输出，含商品快照信息以便前端直接渲染。
 * @Version: 1.1
 * @DateTime: 2026/08/13
 **/
@Data
public class CartItemVO {

    private Long id;
    private Long skuId;
    private Long spuId;
    private String title;
    private String skuSpec;
    private BigDecimal price;
    private Integer quantity;
    private Boolean isSelected;
    private Integer stock;
    private String mainImage;

    /**
     * 组装购物车项VO：以购物车条目为主，叠加 SKU 快照与 SPU 快照的展示字段。
     * 商品快照可能为空（商品服务降级或商品已下架），此时展示字段保留 null。
     * @param po 购物车条目
     * @param sku SKU 快照，可为 null
     * @param spu SPU 快照，可为 null
     * @return 购物车项VO
     */
    public static CartItemVO from(CartItemPO po, SkuSnapshotDTO sku, SpuSnapshotDTO spu) {
        CartItemVO vo = new CartItemVO();
        vo.setId(po.getId());
        vo.setSkuId(po.getSkuId());
        vo.setQuantity(po.getQuantity());
        vo.setIsSelected(po.getSelected() != null && po.getSelected() == 1);
        if (sku != null) {
            vo.setSpuId(sku.getSpuId());
            vo.setSkuSpec(sku.getSkuName());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
        }
        if (spu != null) {
            vo.setTitle(spu.getTitle());
            vo.setMainImage(spu.getMainImageUrl());
        }
        return vo;
    }
}
