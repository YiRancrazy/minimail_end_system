package com.yirancrazy.minimall.cart.vo;

import com.yirancrazy.minimall.api.dto.goods.SkuSnapshotDto;
import com.yirancrazy.minimall.api.dto.goods.SpuSnapshotDto;
import com.yirancrazy.minimall.cart.entity.CartItemPo;
import com.yirancrazy.minimall.common.util.MinioUtil;
import java.math.BigDecimal;
import lombok.Data;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 将持久化购物车记录与可选 SKU、SPU 快照组合为 API 响应视图。对象会把整数勾选编码
 *     转换为 Boolean，并允许远程快照缺失；来源不可用时，标题、价格、规格、库存和主图保持 null。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
public class CartItemVo {

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
     * 创建购物车视图，但不通过 MinIO 解析商品主图.
     *
     * <p>持久化购物车字段始终复制；sku 非空时复制 SKU 字段，spu 非空时复制 SPU 标题和主图。
     *
     * @param po 持久化购物车记录
     * @param sku 可选 SKU 快照
     * @param spu 可选 SPU 快照
     * @return 已填充的购物车视图
     */
    public static CartItemVo from(CartItemPo po, SkuSnapshotDto sku, SpuSnapshotDto spu) {
        return from(po, sku, spu, null);
    }

    /**
     * 创建购物车视图，并按需将 SPU 主图解析为 MinIO 公网 URL.
     *
     * <p>仅当 spu 和 minioUtil 均非空时解析图片。SKU 或 SPU 快照为空时，对应视图字段保持未设置。
     *
     * @param po 持久化购物车记录
     * @param sku 可选 SKU 快照
     * @param spu 可选 SPU 快照
     * @param minioUtil 可选 URL 解析器
     * @return 已填充的购物车视图
     */
    public static CartItemVo from(
            CartItemPo po, SkuSnapshotDto sku, SpuSnapshotDto spu, MinioUtil minioUtil) {
        CartItemVo vo = new CartItemVo();
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
            vo.setMainImage(
                    needResolve ? minioUtil.resolvePublicUrl(spu.getMainImageUrl()) : spu.getMainImageUrl());
        }
        return vo;
    }
}
