package com.yirancrazy.minimall.goods.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 商品搜索视图对象，用于 ES 搜索结果展示
 * @Version: 1.0
 * @DateTime: 2026/08/03
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpuSearchVO {

    /** SPU 主键 ID */
    private Long spuId;

    /** 商品标题 */
    private String title;

    /** 最低价格（分） */
    private Long minPrice;

    /** 最高价格（分） */
    private Long maxPrice;

    /** 主图 URL */
    private String mainImage;

    /** 商家ID */
    private Long merchantId;
}
