package com.yirancrazy.minimall.goods.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: SPU 详情视图对象，聚合 SPU 基础信息与其下 SKU 列表，用于用户端商品详情
 * @Version: 1.0
 * @DateTime: 2026/08/02
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuDetailVO {
    private Long spuId;
    private String spuNo;
    private String title;
    private String subtitle;
    private String mainImageUrl;
    private Long merchantId;
    private Long categoryId;
    private Boolean isOnSale;
    private List<SkuVO> skus;
}
