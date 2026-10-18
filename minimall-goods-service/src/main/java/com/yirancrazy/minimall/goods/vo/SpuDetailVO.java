package com.yirancrazy.minimall.goods.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuDetailVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
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
