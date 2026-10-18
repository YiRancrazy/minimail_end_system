package com.yirancrazy.minimall.cart.vo;

import java.math.BigDecimal;
import lombok.Data;
import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDTO;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDTO;
import com.yirancrazy.minimall.cart.entity.CartItemPO;
import com.yirancrazy.minimall.common.util.MinioUtil;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: CartItemVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
     * 执行 from 方法。
     * @param po po 参数
     * @param sku sku 参数
     * @param spu spu 参数
     * @return 处理结果
     */
    public static CartItemVO from(CartItemPO po, SkuSnapshotDTO sku, SpuSnapshotDTO spu) {
        return from(po, sku, spu, null);
    }


    /**
     * 执行 from 方法。
     * @param po po 参数
     * @param sku sku 参数
     * @param spu spu 参数
     * @param minioUtil minioUtil 参数
     * @return 处理结果
     */
    public static CartItemVO from(CartItemPO po, SkuSnapshotDTO sku, SpuSnapshotDTO spu, MinioUtil minioUtil) {
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
            boolean needResolve = minioUtil != null;
            vo.setMainImage(needResolve
                ? minioUtil.resolvePublicUrl(spu.getMainImageUrl())
                : spu.getMainImageUrl());
        }
        return vo;
    }
}
