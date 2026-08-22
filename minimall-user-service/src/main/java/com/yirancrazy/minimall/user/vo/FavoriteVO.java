package com.yirancrazy.minimall.user.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: yirancrazy@gmail.com
 * @Description: 用户收藏出参VO，供收藏列表展示；商品展示字段由商品服务快照富化，缺失时保留 null 由前端降级
 * @Version: 2.0
 * @DateTime: 2026/08/20
 **/
@Data
@NoArgsConstructor
public class FavoriteVO {
    /** 收藏记录ID */
    private Long id;
    /** 商品SKU ID */
    private Long skuId;
    /** 商品SPU ID */
    private Long spuId;
    /** 商品标题，至少含 SKU 名称，缺失时回退 SPU 标题 */
    private String skuName;
    /** 商品主图URL，来自SPU快照 */
    private String skuImage;
    /** 商品售价 */
    private BigDecimal price;
    /** 收藏时间 */
    private LocalDateTime createTime;
}
