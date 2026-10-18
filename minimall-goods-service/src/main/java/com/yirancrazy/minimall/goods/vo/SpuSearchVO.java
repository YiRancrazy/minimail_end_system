package com.yirancrazy.minimall.goods.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @Author: yirancrazy@gmail.com
 * @Description: SpuSearchVO 视图对象。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuSearchVO {


    private Long spuId;


    private String title;


    private Long minPrice;


    private Long maxPrice;


    private String mainImage;


    private Long merchantId;
}
