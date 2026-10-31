package com.yirancrazy.minimall.goods.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 输出 Elasticsearch 搜索命中的商品摘要、价格区间和主图 URL。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuSearchVo {

  private Long spuId;

  private String title;

  private Long minPrice;

  private Long maxPrice;

  private String mainImage;

  private Long merchantId;
}
