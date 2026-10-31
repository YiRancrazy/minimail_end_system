package com.yirancrazy.minimall.goods.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 输出用户端在售商品详情及其 SKU 列表。
 * @Version: 1.0
 * @DateTime: 2026/10/16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuDetailVo {
  private Long spuId;
  private String spuNo;
  private String title;
  private String subtitle;
  private String mainImageUrl;
  private Long merchantId;
  private Long categoryId;
  private Boolean isOnSale;
  private List<SkuVo> skus;
}
